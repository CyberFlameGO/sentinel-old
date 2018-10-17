#!/usr/bin/env bash
java
  -Xmx16g \
  -Xlog:gc*:logs/gc-%t.log::filesize=1g \
  -jar Sentinel.jar
