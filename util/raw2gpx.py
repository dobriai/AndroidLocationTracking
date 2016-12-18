#!/usr/bin/python3

# To prepare a raw sample:
#   * Go to https://www.gmap-pedometer.com
#   * Start and draw some route - i.e. in add points
#   * Open the browser's debugger and select the script named gmapPedometer_yui.js
#   * Beautify the script, by pressing on the {}-icon
#   * Find the function prepareFieldsForSave(b) and place a break-point in it - e.g. on the return
#   * Back in the web-page hit the save button - this should hit the break-point
#   * In the console type this:
#       for (var a = 0; a < gLatLngArray.length; a++) { console.log("lat=" + gLatLngArray[a].lat() + ", lng=" + gLatLngArray[a].lng()) }
#     which should print out the path in some reasonable raw form
#   * Copy over the result and pipe it into a "raw" file, which is to be fed to thsi script
#   * Remove the unwanted line prefixes of the sort 'VM667:1 ' or similar - the 'raw' file should end up with lines like this:
#     ...
#     lat=45.50762, lng=-73.55676
#     ...
#     and nothing else - i.e. no blank lines, leading spaces - anything!
#   * Run this script on that 'raw' file and pipe output as needed
#
import fileinput
import re

print('''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>

<gpx xmlns="http://www.topografix.com/GPX/1/1" xmlns:gpxx="http://www.garmin.com/xmlschemas/GpxExtensions/v3" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1" creator="Oregon 400t" version="1.1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd">
  <trk>
    <name>Fake GPX Document</name>
    <trkseg>''')

secs=13*60
for line in fileinput.input():
    matchobj = re.search(r'lat=(-?\d+\.\d+), *lng=(-?\d+\.\d+)', line)
    print('      <trkpt lat="{0}" lon="{1}">'.format(matchobj.group(1), matchobj.group(2)))
    print('        <ele>20.46</ele>')
    print('        <time>2016-12-06T09:{:02d}:{:02d}Z</time>'.format(secs // 60, secs % 60))
    print('      </trkpt>')
    secs += 1

print('''    </trkseg>
  </trk>
</gpx>''')
