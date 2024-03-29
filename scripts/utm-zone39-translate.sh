#!/bin/bash
set -e
coordinates=$2
filename=$1

gdal_translate -a_srs '+proj=utm +zone=39 +ellps=intl +towgs84=-143,-236,7,0,0,0,0 +units=m +no_defs'-co COMPRESS=LZW -co PREDICTOR=2 -co TILED=YES -a_ullr $coordinates "./$filename.tif" "$filename.translated.tif"

gdalwarp -s_srs '+proj=utm +zone=39 +ellps=intl +towgs84=-143,-236,7,0,0,0,0 +units=m +no_defs ' -t_srs '+proj=webmerc' -r 'sum' -co 'COMPRESS=LZW' -co 'PREDICTOR=2 "./$filename.translated.tif" "$filename.final.tif"
