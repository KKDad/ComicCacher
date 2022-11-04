#!/bin/sh

cp static_content.xml /jetty/webapps/

# Optionally, copy or move the static content directory inside jetty to prevent any data loss
cp -a /static_content /jetty/static_content/
/jetty/bin/jetty.sh run
