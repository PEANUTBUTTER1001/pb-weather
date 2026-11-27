"""Create a deterministic browser reference without changing the source prototype."""
from pathlib import Path
import hashlib
import json

source = Path('outputs/pb-weather-prototype.html')
output = Path('.tools/design-preview')
output.mkdir(parents=True, exist_ok=True)
html = source.read_text(encoding='utf-8').replace('heroBgMode: "c",', 'heroBgMode: "a",')
test_chrome = '''
<style>
/* Test framing only: remove the simulator/device chrome, retain app component CSS. */
#simDrawer { position:fixed !important; display:block !important; left:430px !important; top:0 !important; width:360px !important; }
* { scrollbar-width:none !important; }
*::-webkit-scrollbar { width:0 !important; height:0 !important; }
#deviceColumn { position:fixed !important; left:0 !important; top:0 !important; width:375px !important; }
#appShell { width:375px !important; height:833px !important; min-height:833px !important; max-height:833px !important; border:0 !important; border-radius:0 !important; box-shadow:none !important; }
</style>

'''
(output / 'reference.html').write_text(html.replace('</head>', test_chrome + '\n</head>'), encoding='utf-8')
(output / 'provenance.json').write_text(json.dumps({
    'source': str(source), 'sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
    'frame': {'width': 375, 'height': 833, 'unit': 'CSS px; Android capture normalized by density'},
    'changes': ['simulator/device chrome only', 'built-in A lightweight static gradient mode selected per 2026-11-27 user instruction', 'native browser scrollbar gutter hidden'],
    'unmodified': ['source file', 'app component CSS', 'prototype data/formatting rules', 'event handlers'],
}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Reference generated: .tools/design-preview/reference.html')
