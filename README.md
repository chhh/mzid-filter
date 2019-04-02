# mzid-filter
Filter MzIdentML according to a delimited text file and inject custom `userParam` entries into it.

## Download:
[Download the latest release link](https://github.com/chhh/mzid-filter/releases/latest)

## Requirements:
- Java 8

## Usage:
To print the usage info:  
`java -jar .\mzid-filter-X.X.jar` 
without any parameters or with `-h`.

Typical usage example:  
`java -jar .\mzid-filter-X.X.jar --map "from-column-01" "to-userParam-01" --map "from-column-02" "to-userParam-02" --mzid <path-to.mzid> --psms <path-to-psm.tsv> --mapf <path-to-mappings-file>`  

## Features:
- Mappings can be specified in a separate tab delimited file, one mapping per line. 
  Specify the path to mappings file via `--mapf` option.  
  An example of a mappings file can be found [here](https://github.com/chhh/mzid-filter/blob/cba81272c8077155699ca6c4d438eb47d081fc04/mappings-example.txt).  
  Mappings that are specified on the command line take precedence over the ones in the file.
- The delimiter in `psm.tsv` file is auto-detected (can be comma, tab, space or semicolon).
- The column in `psm.tsv` that is used for matching PSMs to `mzid` can be provided as a parameter via `--psmidcol` option.
  Defaults to `Spectrum`.
- By default `--filter` and `--prune` are both true.
  - `--filter` removes any PSMs from mzid that are not found in the PSMs file.
  - `--prune` also removes any unused information from mzid. Such as `DBSequence` entries, for example, 
    which might not be referenced anymore after PSM removal by `--filter`.
