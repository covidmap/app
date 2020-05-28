-- array filter function
CREATE TEMP FUNCTION filterArray(items ARRAY<string>)
RETURNS ARRAY<string>
LANGUAGE js AS """
  const filtered = [];
  items.map((item) => {
    if (!!item) filtered.push(item);
  });
  return filtered;
""";

INSERT INTO us.tracked (
    geo,
    ids,
    location,
    contact,
    source,
    altNames,
    owner,
    capabilities,
    name,
    naics,
    status,
    open
)

WITH src AS (
  SELECT
    latitude,
    longitude,
    ST_GEOGPOINT(longitude, latitude) as point,
    state,
    city,
    name,
    objectId,
    rowId,
    address,
    zip,
    telephone,
    type,
    status,
    open,
    county,
    country,
    naicsCode,
    naicsDesc,
    source,
    website,
    filterArray([altName]) as altNames,
    owner,
    ownerType,
    beds,
    filterArray([trauma1, trauma2]) as traumaTypes,
    pediatric,
    helipad

  FROM
    `covid-impact-map.us.facilities`
)

SELECT
  -- geopoint
  STRUCT<point GEOGRAPHY, latitude FLOAT64, longitude FLOAT64, `hash` STRING> (point, latitude, longitude, ST_GEOHASH(point, 20)),

  -- IDs
  STRUCT<object STRING, row STRING, places STRING> (objectId, rowId, NULL),

  -- location
  STRUCT<state STRING, city STRING, county STRING, country STRING, zip STRING, address STRING> (
    state,
    city,
    county,
    country,
    LPAD(CAST(zip as STRING), 5, "0"),
    address),

  -- contact info
  STRUCT<phone STRING, website STRING> (
    telephone,
    website),

  -- sourcing
  source,

  -- alternate names
  altNames,

  -- owner info
  STRUCT<type STRING, spec STRING> (ownerType, owner),

  -- capabilities
  STRUCT<
    beds INT64,
    helipad BOOLEAN,
    trauma STRUCT<
      equipped BOOLEAN,
      pediatric BOOLEAN,
      types ARRAY<string>>> (
    beds,
    helipad,
    (ARRAY_LENGTH(traumaTypes) > 0, pediatric, traumaTypes)
  ),

  -- name
  name,

  -- NAICS / status
  naicsCode, status, open

FROM src
