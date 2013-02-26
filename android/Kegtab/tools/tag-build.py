#!/usr/bin/env python

import datetime
import os
import sys

OUT_HTML_DIR = 'assets/html'
OUT_HTML_LICENSES_FILENAME = 'third_party_licenses.html'

# Lowercase only
LICENSE_FILENAMES = (
  'copying',
  'copying.txt',
  'license',
  'license.txt'
)

def usage():
  sys.stderr.write("Usage: %s <project_dir>\n" % sys.argv[0])
  sys.exit(1)

def main():
  if len(sys.argv) < 2:
    usage()

  project_dir = sys.argv[1]
  
  generate_license_info(project_dir)
  
def generate_license_info(project_dir):
  lib_dir = os.path.join(project_dir, 'libs')
  lib_dirs = os.listdir(lib_dir)
  
  dirs = [os.path.join(lib_dir, d) for d in lib_dirs if not d.startswith('.')]
  dirs = [d for d in dirs if os.path.isdir(d)]
  
  license_map = {}
  for d in dirs:
    library_name = os.path.basename(d)
    license = None
    for f in os.listdir(d):
      if f.lower() in LICENSE_FILENAMES:
        license = os.path.join(d, f)
        license_map[library_name] = license
        break
    if not license:
      print "Warning: no license file found for", library_name
  
  out_dir = os.path.join(project_dir, OUT_HTML_DIR)
  if not os.path.exists(out_dir):
    os.makedirs(out_dir)
  out_file = os.path.join(out_dir, OUT_HTML_LICENSES_FILENAME)
  f = open(out_file, 'w')
  f.write('<html><body style="color: #eee; background: #000;">\n')
  
  for library_name, license_file in license_map.iteritems():
    f.write('<h2>%s</h2><br>\n' % library_name)
    lines = open(license_file).read()
    f.write('<pre>\n')
    f.write(lines)
    f.write('</pre>\n')
    f.write('<hr/>\n\n')

  f.write('</body></html>\n')
  
  print "Generated third party license HTML:", OUT_HTML_LICENSES_FILENAME

if __name__ == '__main__':
  main()
