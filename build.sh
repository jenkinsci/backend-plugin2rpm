#!/bin/bash -e
mvn -e clean install exec:java -Dexec.mainClass=plugin2rpm.App
find target/RPMS -name '*.rpm' | xargs rpm-sign $(cat ~/.gpg.passphrase)
cd target
createrepo .
rsync -avz RPMS repodata hudson-ci.org:~/www/hudson-ci.org/redhat/plugins
