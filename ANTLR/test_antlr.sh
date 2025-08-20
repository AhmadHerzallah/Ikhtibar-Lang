#!/usr/bin/env bash
set -euo pipefail # Exit on error, undefined variables, and pipe failure

usage() {
  echo "Usage: ./test_antlr.sh <input.txt> [output.html]" >&2
}

# Check if no arguments are provided or if the help flag is provided
if [[ ${1:-} == "-h" || ${1:-} == "--help" || $# -lt 1 || $# -gt 2 ]]; then
  usage
  exit $([[ $# -lt 1 || $# -gt 2 ]] && echo 1 || echo 0)
fi

INPUT_FILE="$1"
OUTPUT_FILE="${2:-}"

# Check if the input file exists
if [[ ! -f "$INPUT_FILE" ]]; then
  echo "error: input file not found: $INPUT_FILE" >&2
  exit 1
fi

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ANTLR_DIR="$SCRIPT_DIR"
JAR_PATH="$ANTLR_DIR/antlr-runtime-4.13.2.jar"
SRC_DIR="$ANTLR_DIR/gen"
BUILD_DIR="$ANTLR_DIR/out/testbin"

# Check if the ANTLR runtime jar exists
if [[ ! -f "$JAR_PATH" ]]; then
  echo "error: ANTLR runtime jar not found at: $JAR_PATH" >&2
  exit 1
fi

# Create the build directory if it doesn't exist
mkdir -p "$BUILD_DIR"

# Compile the Java sources
echo "Compiling"
javac -d "$BUILD_DIR" -cp "$JAR_PATH" "$SRC_DIR"/*.java

# Run the ARHtmlMain class
echo "Running"
if [[ -n "$OUTPUT_FILE" ]]; then
  java -cp "$BUILD_DIR:$JAR_PATH" ARHtmlMain "$INPUT_FILE" "$OUTPUT_FILE"
else
  java -cp "$BUILD_DIR:$JAR_PATH" ARHtmlMain "$INPUT_FILE"
fi

