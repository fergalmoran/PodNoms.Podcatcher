#!/bin/bash
#tedia2sql -t mysql -o assets/podnoms.sql podnoms.dia
parsediasql --file podnoms.dia --db sqlite3 > assets/podnoms.sql
