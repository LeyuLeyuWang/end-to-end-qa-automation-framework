"""Rebuild the offline textbook from manuscript and repository source.

Requires Python packages Markdown and Pygments. No network access at build/read time.
Run: python docs/textbook/build_textbook.py
"""
from pathlib import Path
import hashlib
import html
import json
import re
from datetime import datetime, timezone
import markdown
from pygments import highlight
from pygments.lexers import get_lexer_by_name
from pygments.formatters import HtmlFormatter

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
FORMATTER = HtmlFormatter(cssclass="codehilite")
CSS = r'''
:root{--bg:#f5f3ee;--paper:#fffefa;--ink:#233431;--muted:#64736c;--line:#d9dfd7;--accent:#226654;--code:#f0f3ef;--size:18px;color-scheme:light}
*{box-sizing:border-box}html{scroll-behavior:auto}body{margin:0;background:var(--bg);color:var(--ink);font-family:Segoe UI,"Microsoft YaHei",sans-serif;font-size:var(--size);line-height:1.9}a{color:var(--accent);text-underline-offset:4px}button,input{font:inherit}button{cursor:pointer;border:1px solid var(--line);background:var(--paper);color:var(--ink);border-radius:8px;padding:5px 10px}button:hover,button:focus-visible{border-color:var(--accent)}:focus-visible{outline:2px solid var(--accent);outline-offset:3px}
.sidebar{position:fixed;inset:0 auto 0 0;width:290px;padding:27px 20px;background:var(--paper);border-right:1px solid var(--line);overflow-y:auto;z-index:20}.brand{font-size:12px;font-weight:700;letter-spacing:2px;color:var(--accent)}.sidebar h2{font-size:24px;line-height:1.45;margin:9px 0}.subtitle{font-size:13px;color:var(--muted)}.search{width:100%;margin:17px 0 8px;padding:10px;border:1px solid var(--line);border-radius:8px;background:var(--bg);color:var(--ink);font-size:14px}.nav a{display:block;text-decoration:none;font-size:14px;line-height:1.65;padding:8px 10px;margin:3px 0;border-radius:6px}.nav a.active{background:#e4eee6;color:#164d3e;font-weight:650}.nav a.done:after{content:' ✓';color:var(--accent)}.search-result{font-size:13px;display:block;border-bottom:1px solid var(--line);padding:10px 0;text-decoration:none}.search-result small{display:block;color:var(--muted);line-height:1.5}#searchStatus{font-size:12px;color:var(--muted)}.side-links{font-size:13px;border-top:1px solid var(--line);padding-top:14px;margin-top:20px;display:grid;gap:5px}
.main{margin-left:290px}.toolbar{position:sticky;top:0;z-index:10;background:var(--paper);border-bottom:1px solid var(--line);padding:10px 32px;display:flex;align-items:center;gap:8px;font-size:13px}.toolbar .spacer{flex:1}.mobile-menu{display:none}#position{color:var(--muted)}.content{max-width:1040px;padding:45px 60px 70px;margin:auto}.eyebrow{color:var(--accent);font-size:12px;letter-spacing:2px;font-weight:700}.intro{padding:0 0 30px;border-bottom:1px solid var(--line);margin-bottom:30px}.intro h1{font-size:39px;line-height:1.3;letter-spacing:-1px;margin:12px 0}.intro p{font-size:16px;color:var(--muted)}.chapter h1{font-size:31px;line-height:1.5;margin:0 0 24px}.chapter h2{font-size:24px;margin:42px 0 15px;padding-top:7px;scroll-margin-top:82px}.chapter h3{font-size:21px;margin:30px 0 12px}p{margin:16px 0}strong{font-weight:700}blockquote{margin:24px 0;padding:12px 22px;background:#eaf1e9;border-left:4px solid var(--accent);font-size:16px}blockquote p{margin:4px 0}code{font-family:Consolas,"Cascadia Code",monospace;font-size:.87em;background:var(--code);padding:2px 5px;border-radius:4px;overflow-wrap:anywhere}.codehilite{background:var(--code);border:1px solid var(--line);border-radius:9px;overflow-x:auto;margin:20px 0}.codehilite pre{padding:20px;margin:0;font-size:14px;line-height:1.7;tab-size:4}.codehilite code{padding:0;background:none;overflow-wrap:normal;font-size:inherit}.table-wrap{overflow-x:auto;margin:23px 0}table{width:100%;border-collapse:collapse;font-size:15px;line-height:1.7}th,td{padding:11px 13px;border:1px solid var(--line);text-align:left;vertical-align:top}th{background:#eaf0e7}tr:nth-child(even) td{background:var(--paper)}details{margin:25px 0;border:1px solid var(--line);border-radius:9px;background:var(--paper);padding:14px 20px}summary{font-weight:650;color:var(--accent);cursor:pointer}details p{font-size:16px}.source-ref{font-size:12px;color:var(--muted);margin-top:24px;overflow-wrap:anywhere}.chapter-bottom{display:flex;justify-content:space-between;gap:12px;border-top:1px solid var(--line);padding-top:25px;margin-top:45px;flex-wrap:wrap;font-size:14px}.local-toc{font-size:14px;margin-bottom:25px;padding:15px 20px;border:1px solid var(--line);border-radius:8px}.local-toc a{display:block;text-decoration:none;padding:3px 0}.progress{height:3px;position:absolute;left:0;bottom:-1px;background:var(--accent);width:0}.skip{position:fixed;top:-100px;left:10px;z-index:100;background:var(--paper);padding:5px}.skip:focus{top:5px}.source-file{scroll-margin-top:30px;margin:50px 0}.source-file h2{font-size:19px;overflow-wrap:anywhere}.source-line{display:block;scroll-margin-top:70px}.source-line:target{background:#fff2a8}.line-number{display:inline-block;width:48px;color:#738478;user-select:none;font-size:12px}.source-body{max-width:1250px;margin:auto;padding:30px}.source-file pre{overflow-x:auto}.source-index{columns:2;font-size:14px}.source-index a{display:block;overflow-wrap:anywhere}.footer{font-size:12px;color:var(--muted);margin-top:30px}.chapter[hidden]{display:none!important}
body.dark{--bg:#17211e;--paper:#1d2a25;--ink:#dce8df;--muted:#acbcb0;--line:#3a4b41;--accent:#96d4ad;--code:#e9eee8;color-scheme:dark}body.dark .codehilite{color:#233431}body.dark blockquote,body.dark th{background:#293d30}body.dark .nav a.active{background:#304b39;color:#d6f3de}body.dark code{color:#233431}body.dark .codehilite code{color:inherit}
@media(max-width:1050px){.sidebar{width:250px}.main{margin-left:250px}.content{padding:35px 30px}.toolbar{padding:10px 22px}.intro h1{font-size:32px}}
@media(max-width:760px){.sidebar{transform:translateX(-100%);width:290px;box-shadow:8px 0 25px #0002}.sidebar.open{transform:translateX(0)}.main{margin-left:0}.mobile-menu{display:inline-block}.content{padding:28px 20px 55px}.toolbar{padding:9px 14px;flex-wrap:wrap}.chapter h1{font-size:27px}.chapter h2{font-size:22px;scroll-margin-top:125px}.intro h1{font-size:31px}.codehilite pre{font-size:13px;padding:15px}#position{font-size:11px}.source-index{columns:1}}
@media print{.sidebar,.toolbar,.chapter-bottom,.local-toc,.intro,.skip{display:none!important}.main{margin:0}.content{max-width:none;padding:0}.chapter[hidden]{display:block!important}.chapter{break-before:page}.chapter:first-of-type{break-before:auto}body{background:white;color:black;font-size:11pt}.codehilite pre{white-space:pre-wrap;font-size:9pt}.codehilite{break-inside:avoid}details{display:block}details>*{display:block}.table-wrap{overflow:visible}a{color:inherit}.footer{display:none}}
'''
CSS += FORMATTER.get_style_defs('.codehilite')

def sid(path):
    return 's-' + hashlib.sha256(path.encode()).hexdigest()[:12]

sources = {}
def extract(match):
    specification = match.group(1)
    path, _, method = specification.partition('#')
    file = (ROOT / path).resolve()
    if not file.is_relative_to(ROOT):
        raise ValueError(path)
    text = file.read_text(encoding='utf-8-sig')
    sources[path] = hashlib.sha256(file.read_bytes()).hexdigest()
    lines = text.splitlines()
    start, end = 0, len(lines)
    if method:
        start = next(i for i, line in enumerate(lines)
                     if re.search(r'^\s*(public|protected|private)\s+.*\b' + re.escape(method) + r'\(', line))
        depth, seen = 0, False
        for i in range(start, len(lines)):
            clean = re.sub(r'"(?:\\.|[^"\\])*"', '""', lines[i])
            for c in clean:
                if c == '{': depth += 1; seen = True
                if c == '}': depth -= 1
            if seen and depth == 0:
                end = i + 1
                break
    language = {'.java':'java','.xml':'xml','.sql':'sql','.yml':'yaml'}.get(file.suffix, 'text')
    return (f'\n<p class="source-ref">原代码 · <a href="source.html#{sid(path)}-L{start+1}" target="_blank" rel="noopener">'
            f'{html.escape(path)} · L{start+1}–L{end}</a></p>\n\n'
            + f'```{language}\n' + '\n'.join(lines[start:end]) + '\n```\n')

chapters = []
combined = ['# QA 自动化：从测试思想到 V5 源码\n\n中英结合 · Java 基础读者 · 源码配套教材\n']
for i, path in enumerate(sorted((HERE/'manuscript').glob('*.md')), 1):
    raw = path.read_text(encoding='utf-8-sig')
    expanded = re.sub(r'\{\{source:([^}]+)\}\}', extract, raw)
    expanded = expanded.replace('<details>', '<details markdown="1">')
    (HERE/'chapters'/path.name).write_text(expanded.replace('href="source.html#','href="../source.html#'), encoding='utf-8')
    combined.append(expanded)
    title = expanded.splitlines()[0].removeprefix('# ')
    md = markdown.Markdown(extensions=['extra','codehilite','toc'],
                           extension_configs={'toc':{'baselevel':1},'codehilite':{'guess_lang':False}})
    body = md.convert(expanded)
    # Prefix heading anchors so chapters can never collide.
    body = re.sub(r'id="([^"]+)"', lambda m: f'id="c{i:02d}-{m.group(1)}"', body)
    toc = re.sub(r'href="#([^"]+)"', lambda m: f'href="#c{i:02d}-{m.group(1)}"', md.toc)
    # Use only section headings in the local contents.
    local = re.findall(r'<h2 id="([^"]+)">(.*?)</h2>', body)
    local_html = '<nav class="local-toc" aria-label="本章目录"><strong>本章路线</strong>' + ''.join(
        f'<a href="#{anchor}">{label}</a>' for anchor,label in local) + '</nav>'
    body = re.sub(r'(<h1[^>]*>.*?</h1>)', r'\1'+local_html, body, count=1)
    body = body.replace('<table>','<div class="table-wrap"><table>').replace('</table>','</table></div>')
    chapters.append({'id':f'c{i:02d}','title':title,'body':body,'text':re.sub('<[^>]+>',' ',body)})

(HERE/'QA_AUTOMATION_TEXTBOOK.md').write_text('\n\n---\n\n'.join(combined),encoding='utf-8')
nav = ''.join(f'<a href="#{c["id"]}" data-chapter="{c["id"]}">{html.escape(c["title"])}</a>' for c in chapters)
sections = []
for i,c in enumerate(chapters):
    prev = f'<a href="#{chapters[i-1]["id"]}">← 上一章</a>' if i else '<span>从这里开始</span>'
    nxt = f'<a href="#{chapters[i+1]["id"]}">下一章 →</a>' if i+1<len(chapters) else '<a href="#c01">回到学习地图</a>'
    sections.append(f'<section class="chapter" id="{c["id"]}" aria-label="{html.escape(c["title"])}">{c["body"]}'
                    f'<div class="chapter-bottom">{prev}<button class="mark" data-id="{c["id"]}">标记本章已读</button>{nxt}</div></section>')

JS = r'''
history.scrollRestoration='manual';
const chapters=[...document.querySelectorAll('.chapter')], nav=[...document.querySelectorAll('[data-chapter]')];
const saved=(key,fallback)=>{try{return JSON.parse(localStorage.getItem(key))??fallback}catch{return fallback}};
const save=(key,value)=>{try{localStorage.setItem(key,JSON.stringify(value))}catch{}};
let done=new Set(saved('qa-book-done',[]));let size=saved('qa-book-size',18);
document.documentElement.style.setProperty('--size',size+'px');
document.body.classList.toggle('dark',saved('qa-book-dark',false));
function updateDone(){nav.forEach(a=>a.classList.toggle('done',done.has(a.dataset.chapter)));document.querySelectorAll('.mark').forEach(b=>b.textContent=done.has(b.dataset.id)?'已读 ✓ · 点击撤销':'标记本章已读')}
function selectChapter(){let hash=decodeURIComponent(location.hash.slice(1)),id=hash.match(/^c\d{2}/)?.[0];if(!chapters.some(c=>c.id===id))id='c01';chapters.forEach(c=>c.hidden=c.id!==id);nav.forEach(a=>a.classList.toggle('active',a.dataset.chapter===id));document.getElementById('intro').hidden=id!=='c01';document.getElementById('position').textContent=chapters.find(c=>c.id===id).getAttribute('aria-label');document.title=document.getElementById('position').textContent+' · QA 教材';save('qa-book-last',id);document.getElementById('sidebar').classList.remove('open');document.getElementById('menu').setAttribute('aria-expanded','false');requestAnimationFrame(()=>{if(hash&&hash!==id){document.getElementById(hash)?.scrollIntoView({behavior:'instant'})}else window.scrollTo({top:0,behavior:'instant'});progress()})}
function progress(){const max=document.documentElement.scrollHeight-innerHeight;document.getElementById('progress').style.width=(max>0?Math.min(100,scrollY/max*100):100)+'%'}
document.addEventListener('click',e=>{const a=e.target.closest('a[href^="#c"]');if(a){e.preventDefault();history.pushState(null,'',a.getAttribute('href'));selectChapter()}});addEventListener('popstate',selectChapter);addEventListener('hashchange',selectChapter);addEventListener('scroll',progress,{passive:true});
document.getElementById('menu').onclick=()=>{let open=document.getElementById('sidebar').classList.toggle('open');document.getElementById('menu').setAttribute('aria-expanded',String(open))};
document.getElementById('theme').onclick=()=>{document.body.classList.toggle('dark');save('qa-book-dark',document.body.classList.contains('dark'))};
document.getElementById('bigger').onclick=()=>{size=Math.min(24,size+1);document.documentElement.style.setProperty('--size',size+'px');save('qa-book-size',size)};
document.getElementById('smaller').onclick=()=>{size=Math.max(15,size-1);document.documentElement.style.setProperty('--size',size+'px');save('qa-book-size',size)};
document.querySelectorAll('.mark').forEach(b=>b.onclick=()=>{done.has(b.dataset.id)?done.delete(b.dataset.id):done.add(b.dataset.id);save('qa-book-done',[...done]);updateDone()});
document.getElementById('search').addEventListener('input',e=>{let query=e.target.value.trim().toLowerCase(),out=document.getElementById('searchResults');out.replaceChildren();document.getElementById('nav').hidden=!!query;if(!query){document.getElementById('searchStatus').textContent='搜索正文、术语或方法名';return}let matches=0;chapters.forEach(c=>{let text=c.textContent.replace(/\s+/g,' '),at=text.toLowerCase().indexOf(query);if(at<0)return;matches++;let a=document.createElement('a');a.href='#'+c.id;a.className='search-result';a.textContent=c.getAttribute('aria-label');let small=document.createElement('small');small.textContent='…'+text.slice(Math.max(0,at-30),at+100)+'…';a.append(small);out.append(a)});document.getElementById('searchStatus').textContent=matches?`${matches} 章包含“${e.target.value.trim()}”`:'没有找到，试试中文或英文术语'});
addEventListener('keydown',e=>{if(e.key==='Escape'){document.getElementById('sidebar').classList.remove('open');document.getElementById('menu').setAttribute('aria-expanded','false')}});
addEventListener('load',selectChapter);
if(!location.hash){let last=saved('qa-book-last','c01');history.replaceState(null,'','#'+(chapters.some(c=>c.id===last)?last:'c01'))}updateDone();selectChapter();
'''
page = f'''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>QA 自动化 · 源码教材</title><style>{CSS}</style></head><body>
<a class="skip" href="#main">跳到正文</a><aside class="sidebar" id="sidebar"><div class="brand">QA ENGINEERING / FIELD GUIDE</div><h2>从测试思想<br>读到项目源码</h2><div class="subtitle">V5 · Java 基础读者 · 中英结合</div><label for="search" class="subtitle">查找知识点</label><input id="search" class="search" type="search" placeholder="例如：回滚、ThreadLocal、PATCH" autocomplete="off"><div id="searchStatus" role="status">搜索正文、术语或方法名</div><div id="searchResults"></div><nav class="nav" id="nav" aria-label="教材章节">{nav}</nav><div class="side-links"><a href="source.html">原代码全览 ↗</a><a href="QA_AUTOMATION_TEXTBOOK.md" download>下载 Markdown 原稿</a><span>已读标记保存在此浏览器；不是掌握程度评分。</span></div></aside>
<main class="main" id="main"><header class="toolbar"><button id="menu" class="mobile-menu" aria-label="切换章节目录" aria-expanded="false">☰ 目录</button><span id="position">学习地图</span><span class="spacer"></span><button id="smaller" aria-label="缩小正文字号">A−</button><button id="bigger" aria-label="增大正文字号">A＋</button><button id="theme" aria-label="切换明暗主题">明 / 暗</button><div class="progress" id="progress"></div></header>
<div class="content"><div class="intro" id="intro"><div class="eyebrow">READ · REASON · VERIFY</div><h1>不只让测试运行，<br>还要知道它证明了什么。</h1><p>19 章循序讲解 · 原代码自动提取 · 练习与参考答案<br>从 Java 基础出发，理解 Web、API、数据库与测试工程。</p></div>{''.join(sections)}<div class="footer">源码快照教材 · 完整 46 个业务场景 · V3 移动端未实现 · GitHub 云端运行尚待验收<br>无需账号、联网或安装阅读工具。浏览器存储受限时，阅读仍可用，进度可能不保存。</div></div></main><script>{JS}</script></body></html>'''
(HERE/'index.html').write_text(page,encoding='utf-8')

all_files = sorted(list((ROOT/'src').rglob('*.java')) + [ROOT/p for p in ['pom.xml','testng.xml','testng-web-parallel.xml','docker/init/init.sql','docker/docker-compose.yml','.github/workflows/qa-tests.yml']])
source_sections, source_index = [], []
for file in all_files:
    path = file.relative_to(ROOT).as_posix()
    sources[path] = hashlib.sha256(file.read_bytes()).hexdigest()
    text = file.read_text(encoding='utf-8-sig')
    language = {'.java':'java','.xml':'xml','.sql':'sql','.yml':'yaml'}[file.suffix]
    marked = highlight(text,get_lexer_by_name(language),HtmlFormatter(nowrap=True)).splitlines()
    ident = sid(path)
    code = ''.join(f'<span class="source-line" id="{ident}-L{i}"><span class="line-number">{i}</span>{line}</span>' for i,line in enumerate(marked,1))
    source_index.append(f'<a href="#{ident}">{html.escape(path)}</a>')
    source_sections.append(f'<section class="source-file" id="{ident}"><h2>{html.escape(path)}</h2><div class="codehilite"><pre>{code}</pre></div></section>')
source_page = f'<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>原代码全览 · QA 教材</title><style>{CSS}</style></head><body><main class="source-body"><a href="index.html">← 返回教材</a><h1>原代码全览</h1><p>来自当前仓库的完整源文件。行号与生成时的源码一致；浏览器可用 Ctrl+F 查找方法名。</p><nav class="source-index">{"".join(source_index)}</nav>{"".join(source_sections)}</main></body></html>'
(HERE/'source.html').write_text(source_page,encoding='utf-8')
manifest = {'generated_at_utc':datetime.now(timezone.utc).isoformat(),'chapters':len(chapters),'source_sha256':sources,
            'chinese_characters_manuscript':sum(len(re.findall('[\u4e00-\u9fff]',p.read_text(encoding='utf-8-sig'))) for p in (HERE/'manuscript').glob('*.md'))}
(HERE/'source-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'chapters':len(chapters),'source_files':len(sources),'chinese_characters':manifest['chinese_characters_manuscript'],'html_bytes':(HERE/'index.html').stat().st_size},ensure_ascii=False))
