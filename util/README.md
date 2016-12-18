Utulities
================

Here is some hndy stuff for testing the app - mostly on an emulator. This directory is not used directly by the Gradle build process or the Android App! 


`udp_svr.py`
------
A trivial UDP server that just dumps on the console. The standard `nc` seems to have bind to the first connection and not do anything thereafter, so it is not good here. Did not have time to delve into why, but the Python server works as I want it to.

`raw2gpx.py`
-------
A "generator" of `.gpx` files for simulating GPS updates. Don't worry about it - just use the supplied `.gpx` files with fake routes.
