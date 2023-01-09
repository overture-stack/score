# ICGC DCC - Storage - Change Log

Change log for the ICGC storage system

1.0.23
--
 - Updated (default) **``filename``** output layout. Files are no longer written as:
```
    .  
    └── output-dir  
        ├── filename-1  
        │   └── object-id  
        └── filename-2  
            └── object-id  
``` 
   but instead:  
```
    .
    └── output-dir
        ├── filename-1
        └── filename-2
```

   if duplicate filenames (but different object id's) are encountered, warning messages will be displayed/logged

1.0.22
--
 - Added support for Azure

1.0.14
--
 - Batch Slicing support in View command
 - Validate repository values in download manifest files against client profile
 - Remove duplicates in result pagination
 - Add client check for correct Java version 

1.0.13
--
 - Fix end-of-stream bug in data channel 

1.0.12
--
 - Fix HTTP timeout settings not being applied

0.0.20
--
 - Fix `--force` overwrite option
