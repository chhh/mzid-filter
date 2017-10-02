# mzid-filter
Filter MzIdentML according to a delimited text file and inject custom `userParam` entries into it.

## Download:
[Latest release link](https://github.com/chhh/mzid-filter/releases/latest)

## Usage:
To print the usage info:  
`java -jar .\mzid-filter-1.1.jar` 
without any parameters or with `-h`.

Typical usage example: 
`java -jar .\mzid-filter-1.1.jar --map "FROM:column01" "TO:userParam01" --map "FROM:column02" "TO:userParam02" --mzid <path-to.mzid> --psms <path-to-psm.tsv>`  

## Features:
- Mappings can be specified in a separate tab delimited file, one mapping per line. 
  Specify the path to mappings file via `--mapf` option.
- The delimiter in `psm.tsv` file is auto-detected (can be comma, tab, space or semicolon).
- The column in `psm.tsv` that is used for matching PSMs to `mzid` can be provided as a parameter via `--psmidcol` option.
  Defaults to `Spectrum`.
- By default `--filter` and `--prune` are both true.
  - `--filter` removes any PSMs from mzid that are not found in the PSMs file.
  - `--prune` also removes any unused information from mzid. Such as `DBSequence` entries, for example, 
    which might not be referenced anymore after PSM removal by `--filter`.
