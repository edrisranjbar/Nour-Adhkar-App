# City search data

The bundled `app/src/main/assets/cities.tsv` is derived from GeoNames `cities15000.zip` and `admin1CodesASCII.txt`, downloaded on 2026-10-17:

- https://download.geonames.org/export/dump/cities15000.zip
- https://download.geonames.org/export/dump/admin1CodesASCII.txt
- Source documentation: https://download.geonames.org/export/dump/readme.txt
- License: Creative Commons Attribution 4.0, https://creativecommons.org/licenses/by/4.0/
- Attribution: GeoNames, https://www.geonames.org/

Changes: unused fields are removed; region names are joined; search aliases are normalized for case, diacritics, Persian/Arabic character variants and zero-width non-joiners. Coordinates are preserved from the source. This is the source's major-city/capital extract, not a complete village/street gazetteer. Results display GeoNames attribution in the app.

To regenerate, download the two source files into `.tooling/` and run `tools/Import-CityCatalog.ps1`. Data is provided by GeoNames as-is without guarantees of completeness or accuracy.

When a city is not bundled, the app still tries Android Geocoder and then the Open-Meteo geocoding service. Open-Meteo's results also use GeoNames data: https://open-meteo.com/en/docs/geocoding-api
