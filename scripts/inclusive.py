#!/usr/bin/env python3
"""Inclusive share of samples whose stack contains a given frame, for the frames that answer 'where does Weld spend time'."""
import sys

FRAMES = [
    ('bean resolution (BeanManagerImpl.getBeans)', 'BeanManagerImpl.getBeans'),
    ('bean resolution (TypeSafeBeanResolver / Resolvable)', ('TypeSafeBeanResolver', 'ResolvableBuilder')),
    ('RESTEasy CdiConstructorInjector', 'CdiConstructorInjector'),
    ('client proxy context lookup (ContextBeanInstance.getInstance)', 'ContextBeanInstance.getInstance'),
    ('interception stack (InterceptionDecorationContext)', 'InterceptionDecorationContext'),
    ('interceptor chain (InterceptorMethodHandler)', 'InterceptorMethodHandler'),
    ('request context activation interceptor', 'AbstractActivateRequestContextInterceptor'),
    ('request context lookup (isActive / getActiveContext)', ('AbstractManagedContext.isActive', 'getActiveContext')),
    ('BeanManagerImpl.getReference / getContext', ('BeanManagerImpl.getReference', 'BeanManagerImpl.internalGetContext')),
    ('any org.jboss.weld frame', 'org.jboss.weld'),
]
total = 0
hits = [0] * len(FRAMES)
for line in open(sys.argv[1]):
    line = line.rstrip('\n')
    stack, _, n = line.rpartition(' ')
    try:
        n = int(n)
    except ValueError:
        continue
    total += n
    for i, (_, pat) in enumerate(FRAMES):
        pats = pat if isinstance(pat, tuple) else (pat,)
        if any(p in stack for p in pats):
            hits[i] += n
print(f'\nInclusive share of samples (total {total}):\n')
for (label, _), h in zip(FRAMES, hits):
    print(f'- {100 * h / total:5.1f}%  {label}')
