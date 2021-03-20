#!/bin/bash

mvn clean install
rm -fr tmp
mkdir -p tmp
cd tmp
unzip ../target/ovpn2yealink-app.zip
cd ovpn2yealink

sed -e 's/^remote.*/remote 1.2.3.4 1194/g' config/vpn.cnf > config/tmp.cnf
mv config/tmp.cnf config/vpn.cnf

./ovpn2yealink.jar -i /Users/th/Desktop/voiptest2.ovpn -o test-vpn.tar -p hallo4321

mkdir test
cd test
tar xvf ../test-vpn.tar
ls -lh 
cat vpn.cnf
