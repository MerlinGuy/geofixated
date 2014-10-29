mkdir /media/geof_stor
mkdir /media/geof_stor/stage
chown -R tomcat6:tomcat6 /media/geof_stor
sed -ie 's/<location of upload staging directory>/\/media\/geof_stor\/stage/g' /var/lib/tomcat6/webapps/geof/WEB-INF/conf/geof.conf
sudo service tomcat6 restart