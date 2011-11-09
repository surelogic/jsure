#!/usr/bin/env bash

# Determine the directory the script is located in (SCRIPT_PATH)
SCRIPT_PATH="${BASH_SOURCE[0]}";
if([ -h "${SCRIPT_PATH}" ]) then
  while([ -h "${SCRIPT_PATH}" ]) do SCRIPT_PATH=`readlink "${SCRIPT_PATH}"`; done
fi
pushd . > /dev/null
cd `dirname ${SCRIPT_PATH}` > /dev/null
SCRIPT_PATH=`pwd`;
popd  > /dev/null

echoHeader(){
  local title=$1
  echo "------------------------------------------------------------"
  echo "-- $title"
  echo "------------------------------------------------------------"
}

echoSubHeader(){
  local title=$1
  echo "-- $title"
}

#
# Check that we have the tools we need to be successful.
#
echoHeader "NEEDED TOOL CHECK (will fail if we are missing something)"
java -version
javac -version
ant -version
echo "Subversion `svn --version --quiet`"
echo "Script is in $SCRIPT_PATH"

echoHeader "CHECK OUT JSURE TOOL FROM SVN"
JSURE_SVN_PROJECTS_LIST_FILE="$SCRIPT_PATH/lib/minimal-svn-project-list.txt"
SVN_REPO="https://fluid.surelogic.com/svn/eng"
echo -n "Enter your $SVN_REPO username: "
read SVN_USER
echo -n "Enter your $SVN_REPO password: "
read -s SVN_PASS
echo
echo

while read LINE
do
  echoSubHeader "Checking out $LINE from $SVN_REPO"
  svn checkout "$SVN_REPO/trunk/$LINE" $LINE --username $SVN_USER --password $SVN_PASS --non-interactive --trust-server-cert
done < "$JSURE_SVN_PROJECTS_LIST_FILE"

echo "Done"
