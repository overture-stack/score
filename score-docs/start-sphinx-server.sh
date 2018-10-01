#!/usr/bin/env bash
sphinx-autobuild \
-b html \
-H localhost \
--watch source \
--ignore "*.swp" \
--ignore "*.pdf" \
--ignore "*.log" \
--ignore "*.out" \
--ignore "*.toc" \
--ignore "*.aux" \
--ignore "*.idx" \
--ignore "*.ind" \
--ignore "*.ilg" \
--ignore "*.tex" \
--ignore "*.pyc" \
--ignore "*.py_*" \
--ignore "*.rst_*" \
--ignore "*4913*" \
-c source source _build/html