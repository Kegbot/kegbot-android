#!/usr/bin/env python

import datetime
import os
import sys

def usage():
  sys.stderr.write("Usage: %s <filename.xml>\n" % sys.argv[0])
  sys.exit(1)

def main():
  if len(sys.argv) < 2:
    usage()
  outfile = os.path.join(sys.argv[1], 'res/values/build_strings.xml')

  build_strings = {
    'build_date' : str(datetime.datetime.now()),
  }

  f = open(outfile, 'w')

  f.write('<?xml version="1.0" encoding="utf-8"?>\n')
  f.write('<resources>\n')

  for k, v in build_strings.iteritems():
    f.write('  <string name="%s">%s</string>\n' % (k, v));

  f.write('</resources>\n')
  f.close()

if __name__ == '__main__':
  main()
