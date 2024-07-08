AutoTran
========

Primary repository for AutoTran development

# Dependencies
[available in this project's Google Drive folder](https://drive.google.com/open?id=0BwZYwDmM29HDfmV3RFduTEJlcWJwMEFaMWFtRmcyTjVpSVBUclBzZ0N0NTZxRjY0ejNyUGc):
* S3TransferUtility.zip
* IDMan.apk

# To build and use:
* Clone this repo
* In src/com/cassens/autotran/constants/URLS.java, set HOST_URL_CONSTANT
 (e.g. please use "http://sdgsystems.net/AutoTranAdmin/" (or AutoTranAdmin-demo)
 for development tests, not the production server at elasticbeanstalk.com)
* Extract S3Transfer.zip into the root of your clone
* Install IDMan.apk (or you'll see: "Unfortunately, AutoTran has stopped.")
    [TODO: ResolveActivity() to detect this problem and display helpful msg?
        https://www.youtube.com/watch?t=100&v=HGElAW224dE
    ]
* Build and install AutoTran
* Initially, IDMan will launch. Test w/driver id 9029 and an arbitrary truck id.
* If AutoTran hangs (lacking a WiFi access point), exit and restart it.

# Test data
* driver id: 9029 ("Joseph Bartrum" has left Cassens, so Cassens tests w/his #)
* Valid VINs: 17 chars except {i, o, q}.
  *Normally, VIN[8] is a complicated checksum;
        But, all '1's and all '2's happen to pass.
  * With VIN[0]='Z' and VIN[8]=0 (check digit), then anything goes.
  * Tip: Keep a VIN in the clipboard so you can paste instead of retyping it.
* Shuttle VIN "Production Status" and "Route" fields can be arbitrary strings.

# "Scan VIN" emulation
To instantly fill any VIN field with a VIN, rather than having to manually enter
or print and scan VINs, change the first parameter of EPX-B DummyScanDriver's
<code>sendScanStatus("Hello " + counter, SYMBOLOGY_UPCA);</code>
so the driver returns VINs ()instead of "Hello #"). Some examples include:

* An array of VINs, indexed by <code>[counter]</code>
* <code>"ZXXXXXXX0XXXX"+String.format("%04d",counter)</code>, where the 'X's
are arbitrary, and thus may be used to distinguish your test VINs from others.

Apparently, any Symbology may be used, so the second parameter can be unchanged.

# Known IDMgr problems with non-standard devices
* Nexus 7 AVD: Upon pressing the "Confirm" button, you may see:
"Unfortunately, Cassens ID Manager has stopped" (because the AVD lacks WiFi?)
BUT, restarting AutoTran will succeed.
* Nexus 5: the "Confirm"" button label is obscured by the "Scan" button
