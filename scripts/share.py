#!/usr/bin/env python3
"""Where the samples of an async-profiler collapsed file go: share of stacks that contain Weld, RESTEasy, the JDK only,
plus the hottest self frames inside Weld.  usage: scripts/share.py <collapsed.txt>"""
import collections, sys

path = sys.argv[1]
total = 0
groups = collections.Counter()
weld_self = collections.Counter()
for line in open(path):
    line = line.rstrip()
    if not line:
        continue
    stack, _, n = line.rpartition(" ")
    n = int(n)
    total += n
    frames = [f.replace("/", ".") for f in stack.split(";")]
    has_weld = any(f.startswith("org.jboss.weld") for f in frames)
    has_rest = any(f.startswith("org.jboss.resteasy") for f in frames)
    if has_weld:
        groups["stacks containing org.jboss.weld"] += n
        weld_frames = [f for f in frames if f.startswith("org.jboss.weld")]
        weld_self[weld_frames[-1]] += n  # deepest Weld frame of the stack
    if has_rest:
        groups["stacks containing org.jboss.resteasy"] += n
    if any(f.startswith("bench.jaxrs") for f in frames):
        groups["stacks containing the benchmark's own code"] += n
print(f"#### {path} ({total} samples)\n")
for k, n in groups.most_common():
    print(f"- {100 * n / total:.1f}% {k}")
print("\nDeepest Weld frame of the stacks that enter Weld:\n")
for f, n in weld_self.most_common(15):
    print(f"- {100 * n / total:.1f}% `{f}`")
print()
