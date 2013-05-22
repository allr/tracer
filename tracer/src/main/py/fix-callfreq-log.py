#!/usr/bin/python

import sys
import re

from collections import defaultdict
from decimal import *

COORDS_RE_STR = r"([^ ]+\s){4}"

def split_bar_coords(l):
    m = re.match(r"(%s)(.*)" % COORDS_RE_STR, l)
    if m is None:
        return None
    try:
        coords = map(Decimal, m.group(1).strip().split())
        rest = m.group(3)
        return (coords, rest)
    except:
        return None


def bg_coords_lines(lines):
    bg_prev = False
    for l in lines:
        yield l, (bg_prev and split_bar_coords(l) is not None)
        if l.startswith('/'):
            bg_prev = l.startswith('/bg')


def fix(fname):
    bar_from_cnt = defaultdict(int)
    lines = None
    f = None
    try:
        f = open(fname, 'r')
        lines = map(lambda s: s.rstrip(), f.readlines())
        for l, bg_prev in bg_coords_lines(lines):
            if bg_prev:
                coords_rest = split_bar_coords(l)
                if coords_rest is not None:
                    coords = coords_rest[0]
                    bar_from_cnt[coords[1]] += 1
        top2 = [(k, bar_from_cnt[k]) for k in
                sorted(bar_from_cnt, key=bar_from_cnt.get, reverse=True)[:2]]
        delta = reduce(lambda x, y: y - x, sorted(zip(*top2)[0]))
        print >> sys.stderr, 'Delta =', delta
    finally:
        if f is not None:
            f.close()

    f = None
    try:
        f = open(fname, 'w')
        for l, bg_prev in bg_coords_lines(lines):
            if bg_prev:
                coords_rest = split_bar_coords(l)
                if coords_rest is not None:
                    coords, rest = coords_rest
                    if coords[1] < 0:
                        coords[1] += delta
                        coords[3] -= delta
                        print >> f, ' '.join(map(str, coords)), rest
                        continue
                    if coords[3] <= 0:
                        continue
            if re.search(r'findfont \d ', l) is not None:
                print >> f, re.sub(r'findfont \d ', 'findfont 12 ', l)
            else:
                print >> f, l
    finally:
        if f is not None:
            f.close()


def main(args):
    if len(args) < 1:
        print >> sys.stderr, "Usage: <ps_file> ..."
    for fname in args[1:]:
        fix(fname)


if __name__ == '__main__':
    main(sys.argv)
