## Public ID Converter
Easily convert public IDs when porting smali files. 

### Features

- Search smali file for all IDs contained in smali file
- Convert all IDs within smali file to new IDs
- Search for a custom ID, useful for framework IDs (still might need some work)

### How-to

- Open jar file and select neccessary files
- You can either find IDs to see what's going to be converted, or run the conversion
- Can also be run from the command line with:

        public_id_convert f[ind] [options] <source public.xml> <source smali>
        public_id_convert c[onvert] [options] <source public.xml> <source smali> <port public.xml>
        -s       The string that is searched for (default is 0x7f)

    Example:
        public_id_convert c original/public.xml original/App.smali mine/public.xml

### Notes
You must have the neccessary resource files already added to the public.xml that you're porting ***to***

You can do this by add the files (drawables, strings, etc.) then building the apk. Decompile the newly built apk and copy the new public.xml.

This tool can help as you can search for which resource files are contained within the smali file 