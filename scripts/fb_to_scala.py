#!/usr/bin/env python

# Convert Facebook Graph API parameter data structure to Scala data structure

from itertools import islice
import os

infile = "api.txt"

os.system("sed -i '/^Default$/d; /^Deprecated$/d; /^Core$/d' " + infile)
with open(infile) as api, open("output", "w") as output:
    while True:
        next_3_lines = list(islice(api, 3))
        if not next_3_lines:
            break
        name = next_3_lines[0].strip()
        comment = next_3_lines[1].strip()
        if "Deprecated" in comment:
            continue
        para_type = next_3_lines[2].strip()
        para_type = para_type.replace('numeric string', 'String')
        para_type = para_type.replace('string', 'String')
        para_type = para_type.replace('datetime', 'String')
        para_type = para_type.replace('unsigned int32', 'Int')
        para_type = para_type.replace('int', 'Int')
        para_type = para_type.replace('boolean', 'Boolean')
        para_type = para_type.replace('bool', 'Boolean')
        para_type = para_type.replace('float', 'Float')
        para_type = para_type.replace('object[]', 'Array[Any]')
        para_type = para_type.replace('object', 'Any')
        output.write(name + ': ' + para_type + ',\t//' + comment + '\n')
