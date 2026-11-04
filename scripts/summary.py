#!/usr/bin/env python3
"""Markdown table of JMH JSON results: one row per benchmark, one column per variant (score +- error, ratio to the first variant)."""
import json, sys, glob, os

res_dir = sys.argv[1]
order = sys.argv[2].split()
groups = {}  # (threads label) -> benchmark -> variant -> list of (score, err, unit)
for f in sorted(glob.glob(os.path.join(res_dir, '*.json'))):
    # <variant>.<tag>.round<N>.json
    name = os.path.basename(f)[:-5]
    variant, tag, _ = name.rsplit('.', 2)
    for r in json.load(open(f)):
        b = r['benchmark'].split('.')[-2] + '.' + r['benchmark'].split('.')[-1]
        m = r['primaryMetric']
        groups.setdefault(tag, {}).setdefault(b, {}).setdefault(variant, []).append((m['score'], m['scoreError'], m['scoreUnit']))

for tag, benches in groups.items():
    print(f'\n### {tag}\n')
    print('| benchmark | ' + ' | '.join(order) + ' |')
    print('|---|' + '---|' * len(order))
    for b, vs in sorted(benches.items()):
        base = None
        cells = []
        for v in order:
            xs = vs.get(v)
            if not xs:
                cells.append('n/a'); continue
            score = sum(x[0] for x in xs) / len(xs)
            err = max(x[1] for x in xs)
            unit = xs[0][2]
            if base is None:
                base = score
                cells.append(f'{score:,.1f} ±{err:,.1f} {unit}')
            else:
                cells.append(f'{score:,.1f} ±{err:,.1f} ({score / base:.2f}x)')
        print(f'| {b} | ' + ' | '.join(cells) + ' |')
