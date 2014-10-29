#!/bin/bash

function getNewPassword() {
    read -s -p "Enter new password " PWD1
    read -s -p "Re-enter password " PWD2
    if [ "$PWD1" -ne "$PWD2" ]; then
        echo "Passwords do not match"
    else
        echo PWD1
    fi
}

MATCH_ERR="Passwords do not match"

function validatePassword () {
    pass=$1
    LEN=${#pass}
    if  [[ $pass =~ [0-9] ]] && [[ $pass =~ [a-z] ]] && [[ $pass =~ [A-Z] ]]  && [[ "$LEN" -ge 8 ]]; then
        vpReturn="y"
    else
        vpReturn="n"
    fi
}

chgPWD='n'
function changePwd() {
    read -p "Configure admin password? (y/n) " chgPWD

    while [ "$chgPWD" = "y" ]; do
        read -s -p "Enter new password " PWD1
        printf "\n"
        read -s -p "Re-enter password " PWD2
        if [ "$PWD1" != "$PWD2" ]; then
            printf "\n$MATCH_ERR\n"
            read -p "Try again? (y/n) " chgPWD
        else
            validatePassword $PWD1
            if [ "$vpReturn" = "y" ]; then
                chgPWD="x"
            else
                printf "\nInvalid password!\n"
                printf "Must be minumum 8 Characters long\n"
                printf "Have at least 1 Uppercase letter\n"
                printf "Have at least 1 Lowercase letter\n"
                printf "Have at least 1 Numeric letter\n"
                read -p "Try again? (y/n) " chgPWD
            fi
        fi
    done

}

read -p "Create storage locations? (y/n) " RESP

if [ "$RESP" = "y" ]; then
  ./create_store.sh
else
  echo "You will need to create the file storage locations manually,"
  service tomcat6 restart
fi

sudo ./geof_admin.sh -createRsa

## Check to see if user wants to change Admin Password now

changePwd

if [ "$chgPWD" =  "x" ]; then
    sudo ./geof_admin.sh -updatePwd -u admin -p $PWD1
fi

## Update the geof.conf file to change the server's email account credentials

CREDS='n'
CONF_FILE="/var/lib/tomcat6/webapps/geof/WEB-INF/conf/geof.conf"

read -p "Configure Geofixated email credentials? (y/n) " RESP

if [ "$RESP" = "y" ]; then
    read -p "Enter email address for server to use? " EMAIL
    sed -i 's/<your email drop address here>/'$EMAIL'/g' "$CONF_FILE"
    read -p "Enter email password that account? " PWD
    sed -i "s/<your email password here>/$PWD/g" "$CONF_FILE"
    CREDS='y'
fi

## Check to see if user wants to change Admin email and RSA key

read -p "Configure admin email address? (y/n) " RESP
if [ "$RESP" = "y" ]; then
    read -p "Enter new admin gmail: " RESP
    sudo ./geof_admin.sh -updateEmail -u admin -e $RESP

    if [ "$CREDS" = "y" ]; then
        read -p "Send RSA Key to new admin email address? (y/n) " RESP
        if [ "$RESP" = "y" ]; then
            sudo ./geof_admin.sh -emailRsa -u admin
        fi
    fi
fi


printf 'Install time: %s\n' $(timer $start_time)

