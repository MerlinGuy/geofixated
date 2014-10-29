#!/bin/bash

function check {
    tmp=`dpkg-query -W -f='${Status}' $pkg`
    if [[ ${tmp} == "install ok installed" ]]; then
        echo ""
        echo "$pkg is installed"
    else
        echo ""
        echo "installing $pkg"
        apt-get -y install $pkg > /dev/null
    fi
}

function timer() {
     if [[ $# -eq 0 ]]; then
         echo $(date '+%s')
     else
         local  stime=$1
         etime=$(date '+%s')

         if [[ -z "$stime" ]]; then stime=$etime; fi

         dt=$((etime - stime))
         ds=$((dt % 60))
         dm=$(((dt / 60) % 60))
         dh=$((dt / 3600))
         printf '%d:%02d:%02d' $dh $dm $ds
     fi
}

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

start_time=$(timer)

echo "................................................."
echo ""
echo "Starting installation of the Geofixated software"
echo "This may take some time depending on your internet connection"
echo ""
echo "................................................."

INSTALL_LOC="http://www.geofixated.org/install"

wget -N "$INSTALL_LOC/geof_pkgs"
wget -N "$INSTALL_LOC/create_store.sh"
chmod +x create_store.sh

cat geof_pkgs | while read pkg; do
	if [ "$pkg" ]; then
		if [ ${pkg::1} != "#" ]; then
		    check $pkg
		fi
	fi
done

echo "Now starting database setup and install"

# Place new ip block in  th pg_hba.conf file to allow connections from out the server
echo "host    all         all         192.168.1.1/24        trust" >> /etc/postgresql/9.1/main/pg_hba.conf

# Replace the localhost address with any
sed -i.bak s/#listen_addresses\ =\ \'localhost\'/listen_addresses\ =\ \'*\'/g /etc/postgresql/9.1/main/postgresql.conf

# Restart the postgresql database service
sudo /etc/init.d/postgresql restart

echo "Installing geofdb now"
# Now start the database setup
wget -N $INSTALL_LOC/database/create_geofdb.sql
wget -N $INSTALL_LOC/database/setup_geofdb.sql
wget -N $INSTALL_LOC/database/create_geofdb_tables.sql

echo ""
echo "Configuring geofdb"
echo "... installing create_geofdb.sql"
sudo -u postgres psql -f create_geofdb.sql >&/dev/null
echo "... installing plpgsql"
sudo -u postgres createlang plpgsql geofdb >&/dev/null
echo "... installing setup_geofdb.sql"
sudo -u postgres psql -f setup_geofdb.sql >&/dev/null

echo "Installing geof webservice now"
cwd=$(pwd)
mkdir /var/lib/tomcat6/webapps/geof
cd /var/lib/tomcat6/webapps/geof
mkdir logs
wget -N $INSTALL_LOC/geof_ws_install.tar.gz
tar zxf geof_ws_install.tar.gz
chown -R tomcat6:tomcat6 /var/lib/tomcat6/webapps/geof
rm -f geof_ws_install.tar.gz
cd $cwd

echo ""
echo ""

read -p "Create default storage locations now? (y/n) " RESP
if [ "$RESP" = "y" ]; then
  ./create_store.sh
else
  echo "You will need to create the file storage locations manually,"
fi

chmod +x /var/lib/tomcat6/webapps/geof/WEB-INF/lib/geof_admin.sh
cp /var/lib/tomcat6/webapps/geof/WEB-INF/lib/geof_admin.sh .

echo "Creating default RSA key"
sudo ./geof_admin.sh -action createRsa
echo "... RSA key created"
echo ""

## Check to see if user wants to change Admin Password now
changePwd

if [ "$chgPWD" = "x" ]; then
    sudo ./geof_admin.sh -action updatePwd -u admin -p $PWD1
fi

HAS_ADMIN_EMAIL="n"
read -p "Configure admin email address? (y/n) " RESP
if [ "$RESP" = "y" ]; then
    read -p "Enter new admin gmail: " RESP
    sudo ./geof_admin.sh -action updateEmail -u admin -e $RESP
    echo ""
    HAS_ADMIN_EMAIL='y'
fi

## Update the geof.conf file to change the server's email account credentials
CREDS='n'
read -p "Configure Geofixated email credentials? (y/n) " RESP

if [ "$RESP" = "y" ]; then
    echo "This email address is used by the server to send emails to the users"
    echo "It can, but does not have to be the same email address for the admin user."

    CONF_FILE="/var/lib/tomcat6/webapps/geof/WEB-INF/conf/geof.conf"
    read -p "Enter email address for server to use? " EMAIL
    sed -i 's/<your email drop address here>/'$EMAIL'/g' "$CONF_FILE"
    read -p "Enter the password for that account? " PWD
    sed -i "s/<your email password here>/$PWD/g" "$CONF_FILE"

    CREDS='y'
    echo ""
fi

## Check to see if user wants to send the Admin an RSA key

if [ "$CREDS" = "y" ] && [ "$HAS_ADMIN_EMAIL" = "y" ]; then
    read -p "Send RSA Key to new admin email address? (y/n) " RESP
    if [ "$RESP" = "y" ]; then
        sudo ./geof_admin.sh -action emailRsa -u admin
        echo "RSA key sent to admin"
    fi
fi

service tomcat6 restart

printf 'Install time: %s\n' $(timer $start_time)

