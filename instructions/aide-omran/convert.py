# -*- coding: utf-8 -*-
"""Convertit les pages HelpNDoc du site help.omranestate.com en Markdown fidele.

Aucun resume, aucune reformulation : chaque paragraphe, cellule de tableau, liste,
lien et image du corps de page est restitue. Les captures d'ecran (PNG) sont
signalees par leur nom de fichier, elles ne sont pas transcrites.
"""
import json, re, sys, html, os
from html.parser import HTMLParser

PAGES = 'pages'
TOC = json.load(open('_toc.json', encoding='utf-8'))
TOPICS = dict(json.load(open('topics.json', encoding='utf-8')))

def anchor(url):
    return 'page-' + re.sub(r'[^a-z0-9]+', '-', url.replace('.html', '').lower()).strip('-')

class Node:
    def __init__(self, tag, attrs):
        self.tag = tag; self.attrs = dict(attrs); self.children = []
    def text(self):
        return ''.join(c if isinstance(c, str) else c.text() for c in self.children)

class Tree(HTMLParser):
    VOID = {'br', 'img', 'hr', 'input', 'meta', 'link', 'col'}
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.root = Node('root', []); self.stack = [self.root]
    def handle_starttag(self, tag, attrs):
        n = Node(tag, attrs); self.stack[-1].children.append(n)
        if tag not in self.VOID: self.stack.append(n)
    def handle_startendtag(self, tag, attrs):
        self.stack[-1].children.append(Node(tag, attrs))
    def handle_endtag(self, tag):
        for i in range(len(self.stack) - 1, 0, -1):
            if self.stack[i].tag == tag:
                del self.stack[i:]; break
    def handle_data(self, d):
        self.stack[-1].children.append(d)

BOLD_CLASSES = set()   # rempli depuis la CSS quand disponible

def load_css():
    p = 'hnd.content.css'
    if not os.path.exists(p): return
    css = open(p, encoding='utf-8', errors='ignore').read()
    for cls, body in re.findall(r'\.(rvts\d+)\s*\{([^}]*)\}', css):
        if 'bold' in body: BOLD_CLASSES.add(cls)

def inline(node, in_cell=False):
    """Texte inline d'un noeud : conserve <br>, liens, images, gras."""
    out = []
    for c in node.children:
        if isinstance(c, str):
            out.append(re.sub(r'\s+', ' ', c)); continue
        t = c.tag
        if t == 'br':
            out.append('<br>' if in_cell else '\n')
        elif t == 'img':
            src = c.attrs.get('src', ''); alt = c.attrs.get('alt', '').strip()
            out.append('[image : %s%s]' % (os.path.basename(src), (' — ' + alt) if alt else ''))
        elif t == 'a':
            href = c.attrs.get('href', ''); txt = inline(c, in_cell).strip()
            if href.endswith('.html') and not href.startswith('http'):
                out.append('[%s](#%s)' % (txt or href, anchor(href.split('#')[0])))
            elif href.startswith('http'):
                out.append('[%s](%s)' % (txt or href, href))
            else:
                out.append(txt)
        elif t == 'span':
            txt = inline(c, in_cell)
            cls = c.attrs.get('class', '')
            if cls in BOLD_CLASSES and txt.strip():
                txt = '**' + txt.strip() + '**' + (' ' if txt.endswith(' ') else '')
            out.append(txt)
        elif t in ('b', 'strong'):
            txt = inline(c, in_cell)
            out.append('**' + txt.strip() + '**' if txt.strip() else txt)
        elif t in ('table',):
            out.append('\n' + table(c) + '\n')
        elif t in ('ul', 'ol'):
            if in_cell:
                items = [inline(li, True).strip() for li in c.children if not isinstance(li, str) and li.tag == 'li']
                items = [re.sub(r'(<br>\s*)+$', '', x) for x in items if x]
                out.append('<br>' + '<br>'.join(('%d. ' % (k + 1) if c.tag == 'ol' else '- ') + x for k, x in enumerate(items)) + '<br>')
            else:
                out.append('\n' + lst(c, 0) + '\n')
        elif t == 'p':
            txt = inline(c, in_cell).strip()
            out.append((txt + ('<br>' if in_cell else '\n')) if txt else ('' if in_cell else ''))
        elif t == 'div':
            out.append(block(c))
        else:
            out.append(inline(c, in_cell))
    return ''.join(out)

def cell(td):
    txt = inline(td, in_cell=True)
    txt = re.sub(r'(<br>\s*)+$', '', txt.strip())
    txt = re.sub(r'^(<br>\s*)+', '', txt)
    txt = re.sub(r'(<br>\s*){2,}', '<br>', txt)
    txt = txt.replace('|', '\\|').replace('\n', ' ')
    return re.sub(r'\s+', ' ', txt).strip()

def table(tbl):
    rows = []
    def walk(n):
        for c in n.children:
            if isinstance(c, str): continue
            if c.tag == 'tr':
                rows.append([cell(td) for td in c.children if not isinstance(td, str) and td.tag in ('td', 'th')])
            elif c.tag in ('tbody', 'thead', 'tfoot'):
                walk(c)
    walk(tbl)
    rows = [r for r in rows if r]
    if not rows: return ''
    width = max(len(r) for r in rows)
    rows = [r + [''] * (width - len(r)) for r in rows]
    if len(rows) == 1 and width == 1:
        return rows[0][0].replace('<br>', '\n')
    md = ['| ' + ' | '.join(rows[0]) + ' |', '|' + '---|' * width]
    for r in rows[1:]:
        md.append('| ' + ' | '.join(r) + ' |')
    return '\n'.join(md)

def lst(node, depth):
    out = []
    ordered = node.tag == 'ol'; i = 0
    for c in node.children:
        if isinstance(c, str) or c.tag != 'li': continue
        i += 1
        txt = inline(c).strip()
        txt = re.sub(r'\n+', '\n' + '  ' * (depth + 1), txt)
        out.append('  ' * depth + ('%d. ' % i if ordered else '- ') + txt)
    return '\n'.join(out)

HEADING_CLASSES = set()  # classes de titre (taille), detectees par heuristique

def block(node):
    out = []
    for c in node.children:
        if isinstance(c, str):
            if c.strip(): out.append(re.sub(r'\s+', ' ', c).strip() + '\n')
            continue
        t = c.tag
        if t in ('h1', 'h2', 'h3', 'h4', 'h5', 'h6'):
            out.append('\n' + '#' * (int(t[1]) + 1) + ' ' + inline(c).strip() + '\n')
        elif t == 'p':
            txt = inline(c).strip()
            if txt:
                # les titres de section HelpNDoc sont des <p> a une seule span "rvts14"/"rvts12"...
                spans = [x for x in c.children if not isinstance(x, str)]
                cls = spans[0].attrs.get('class', '') if len(spans) == 1 and spans[0].tag == 'span' else ''
                if cls in HEADING_CLASSES and len(txt) < 80 and '\n' not in txt:
                    out.append('\n### ' + txt.strip('*').strip() + '\n')
                else:
                    out.append(txt + '\n')
        elif t == 'table':
            out.append('\n' + table(c) + '\n')
        elif t in ('ul', 'ol'):
            out.append('\n' + lst(c, 0) + '\n')
        elif t == 'div':
            out.append(block(c))
        elif t == 'img':
            out.append(inline(Node('x', []), False) + '[image : %s]\n' % os.path.basename(c.attrs.get('src', '')))
        else:
            txt = inline(c).strip()
            if txt: out.append(txt + '\n')
    return ''.join(out)

def body_of(url):
    p = os.path.join(PAGES, url)
    if not os.path.exists(p) or os.path.getsize(p) == 0: return None
    s = open(p, encoding='utf-8', errors='ignore').read()
    i = s.find('<div class="main-content">')
    j = s.find('id="topic_footer"')
    if i < 0: return ''
    frag = s[i:j if j > 0 else None]
    tr = Tree(); tr.feed(frag)
    md = block(tr.root)
    md = re.sub(r'\n{3,}', '\n\n', md)
    return md.strip()

def toc_tree():
    kids = {}
    for n in TOC:
        kids.setdefault(n['parent'], []).append(n)
    return kids

def path_of(nid, byid):
    out = []
    while nid and nid != '#':
        n = byid[nid]; out.append(n['text']); nid = n['parent']
    return list(reversed(out))

if __name__ == '__main__':
    load_css()
    # heuristique titres : classes des spans seules dans un <p> court et gras — a affiner via CSS
    HEADING_CLASSES.update(sys.argv[1].split(',') if len(sys.argv) > 1 else [])
    kids = toc_tree(); byid = {n['id']: n for n in TOC}
    order = []
    def walk(parent, depth):
        for n in kids.get(parent, []):
            order.append((n, depth)); walk(n['id'], depth + 1)
    walk('#', 0)
    missing = []
    parts = []
    for n, depth in order:
        url = n['a_attr']['href']
        md = body_of(url)
        title = n['text']
        crumbs = ' › '.join(path_of(n['parent'], byid)) if n['parent'] != '#' else ''
        head = '\n\n' + '#' * min(depth + 2, 6) + ' ' + title + ' <a id="%s"></a>\n' % anchor(url)
        head += '_Page : `%s`%s_\n\n' % (url, (' — ' + crumbs) if crumbs else '')
        if md is None:
            missing.append(url); parts.append(head + '_(page non téléchargée)_\n')
        else:
            parts.append(head + (md if md else '_(page vide)_'))
    open('REFERENCE.body.md', 'w', encoding='utf-8').write(''.join(parts))
    json.dump(missing, open('missing.json', 'w'), indent=0)
    print('pages', len(order), 'manquantes', len(missing))
