#!/bin/bash

# based on https://gist.github.com/wsargent/11023607

export PW="secret"
export KEYSTORE="src/main/resources/combi/client.jks"

export CRQ="src/main/resources/combi/client.crq"
export CRT="src/main/resources/combi/client.crt"
export P12="src/main/resources/combi/client.p12"
export PEM="src/main/resources/combi/client.pem"
export PRIVPEM="src/main/resources/combi/client-priv.pem"
export CERTPEM="src/main/resources/combi/client-cert.pem"

export CACRT="src/main/resources/combi/clientca.crt"
export CAP12="src/main/resources/combi/clientca.p12"


# Create a self signed certificate & private key to create a root certificate authority.
keytool -genkeypair -v \
  -alias clientCA \
  -keystore $KEYSTORE \
  -dname "CN=clientCA, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US" \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 2048 \
  -ext KeyUsage="keyCertSign" \
  -ext BasicConstraints="ca:true" \
  -validity 365

# Create another key pair that will act as the client.  We want this signed by the client CA.
keytool -genkeypair -v \
  -alias client \
  -keystore $KEYSTORE \
  -dname "CN=client, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US" \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 2048

# Create a certificate signing request from the client certificate.
keytool -certreq -v \
  -alias client \
  -keypass:env PW \
  -storepass:env PW \
  -keystore $KEYSTORE \
  -file $CRQ

# Make clientCA create a certificate chain saying that client is signed by clientCA.
keytool -gencert -v \
  -alias clientCA \
  -keypass:env PW \
  -storepass:env PW \
  -keystore $KEYSTORE \
  -infile $CRQ \
  -outfile $CRT \
  -ext EKU="clientAuth" \
  -rfc

# Export the client-ca certificate from the keystore.  This goes to nginx under "ssl_client_certificate"
# and is presented in the CertificateRequest.
keytool -export -v \
  -alias clientCA \
  -file $CACRT \
  -storepass:env PW \
  -keystore $KEYSTORE \
  -rfc

# Import the signed certificate back into client.jks.  This is important, as JSSE won't send a client
# certificate if it can't find one signed by the client-ca presented in the CertificateRequest.
keytool -import -v \
  -alias client \
  -file $CRT \
  -keystore $KEYSTORE \
  -storetype JKS \
  -storepass:env PW

# Export the client CA to pkcs12. 
keytool -importkeystore -v \
  -srcalias clientCA \
  -srckeystore $KEYSTORE \
  -srcstorepass:env PW \
  -destkeystore $CAP12 \
  -deststorepass:env PW \
  -deststoretype PKCS12

# Export the client to pkcs12.
keytool -importkeystore -v \
  -srcalias client \
  -srckeystore $KEYSTORE \
  -srcstorepass:env PW \
  -destkeystore $P12 \
  -deststorepass:env PW \
  -deststoretype PKCS12

openssl pkcs12 \
  -in $P12 \
  -out $PEM \
  -passin pass:$PW \
  -passout pass:$PW

openssl pkcs12 \
  -in $P12 \
  -out $PRIVPEM \
  -nocerts \
  -nodes \
  -passin pass:$PW \

openssl pkcs12 \
  -in $P12 \
  -out $CERTPEM \
  -clcerts \
  -nokeys \
  -passin pass:$PW


# Then, strip out the client CA from client.jks. 
keytool -delete -v \
 -alias clientCA \
 -storepass:env PW \
 -keystore $KEYSTORE

# List out the contents of client.jks just to confirm it.
keytool -list -v \
  -keystore $KEYSTORE \
  -storepass:env PW
