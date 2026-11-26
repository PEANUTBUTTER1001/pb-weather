"""Create a deterministic browser reference without changing the source prototype."""
from pathlib import Path
import hashlib
import json

source = Path('outputs/pb-weather-prototype.html')
output = Path('.tools/design-preview')
output.mkdir(parents=True, exist_ok=True)
html = source.read_text(encoding='utf-8')
test_chrome = '''
<style>
/* Test framing only: remove the simulator/device chrome, retain app component CSS. */
#simDrawer { display:none !important; }
#deviceColumn { position:fixed !important; left:0 !important; top:0 !important; width:375px !important; }
#appShell { width:375px !important; height:833px !important; min-height:833px !important; max-height:833px !important; border:0 !important; border-radius:0 !important; box-shadow:none !important; }
</style>
<script>
/* The source particle callback is named frame; unrelated layout RAF callbacks continue. */
const referenceRAF = window.requestAnimationFrame.bind(window);
const referenceFrames = new WeakMap();
window.requestAnimationFrame = function(callback) {
  if (callback.name !== 'frame') return referenceRAF(callback);
  const count = referenceFrames.get(callback) || 0;
  if (count >= 30) { document.documentElement.dataset.referenceFrozen='true'; return 0; }
  if (count === 0) document.documentElement.dataset.referenceFrozen='false';
  referenceFrames.set(callback, count + 1);
  return referenceRAF(callback);
};
</script>
'''
(output / 'reference.html').write_text(html.replace('</head>', test_chrome + '\n</head>'), encoding='utf-8')
(output / 'provenance.json').write_text(json.dumps({
    'source': str(source), 'sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
    'frame': {'width': 375, 'height': 833, 'unit': 'CSS px; Android capture normalized by density'},
    'changes': ['simulator/device chrome only', 'particle frame callback stops after 30 ticks (0.480 s source time)'],
    'unmodified': ['source file', 'app component CSS', 'prototype data/formatting rules', 'event handlers'],
}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Reference generated: .tools/design-preview/reference.html')
