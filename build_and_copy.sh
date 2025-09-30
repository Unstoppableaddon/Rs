#!/usr/bin/env bash
set -e
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven not found. Install Java 17 and Maven." >&2
  exit 1
fi
mvn -q -U clean package
echo "Built target/BattleRoyale-0.1.jar"
read -p "Copy to your server plugins folder? Enter path (or leave blank to skip): " PLUGDIR
if [ -n "$PLUGDIR" ]; then
  cp -f target/BattleRoyale-0.1.jar "$PLUGDIR/BattleRoyale-0.1.jar"
  echo "Copied to $PLUGDIR/BattleRoyale-0.1.jar"
fi
