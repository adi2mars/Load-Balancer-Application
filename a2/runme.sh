#!/bin/bash

case $1 in

    "-c")
        if [ ! -d compiled ]; then
          mkdir compiled
        fi
#        curl -o compiled/json-20231013.jar https://cs.utm.utoronto.ca/~pate1646/csc301/json-20231013.jar

        javac -d ./compiled -cp "./compiled/sqlite.jar:./compiled/json-20231013.jar" ./src/UserService/UserService.java
        javac -d ./compiled/ProductService -cp "./compiled/json-20231013.jar" ./src/ProductService/ProductService.java
        javac -d ./compiled/OrderService -cp "./compiled/json-20231013.jar" ./src/OrderService/OrderService.java
        javac -d ./compiled/ICIS -cp "./compiled/json-20231013.jar" ./src/ICIS/ICIS.java
    ;;

    "-u")
        java -cp "./compiled/sqlite.jar:./compiled/json-20231013.jar" ./src/UserService/UserService.java $(sed -n '4p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') $(sed -n '3p' config.json " " | sed 's/[^0-9]*//g')
    ;;

    "-p")
        java -cp "./compiled/sqlite.jar:./compiled/json-20231013.jar" ./src/ProductService/ProductService.java $(sed -n '16p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') $(sed -n '11p' config.json " " | sed 's/[^0-9]*//g')
    ;;

    "-i")
        icisip=$(sed -n '20p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') 
        icisport=$(sed -n '19p' config.json " " | sed 's/[^0-9]*//g')
        productip=$(sed -n '16p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') 
        productport=$(sed -n '11p' config.json " " | sed 's/[^0-9]*//g')
        userip=$(sed -n '4p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p')         
        userport=$(sed -n '3p' config.json " " | sed 's/[^0-9]*//g')
        java -cp "./compiled/json-20231013.jar" ./src/ICIS/ICIS.java $icisip $icisport $productport $productip $userport $userip
    ;;

    "-o")
        orderip=$(sed -n '8p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') 
        orderport=$(sed -n '7p' config.json " " | sed 's/[^0-9]*//g')
        icisip=$(sed -n '20p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') 
        icisport=$(sed -n '19p' config.json " " | sed 's/[^0-9]*//g')
        java -cp "./compiled/sqlite.jar:./compiled/json-20231013.jar" ./src/OrderService/OrderService.java $orderip $orderport $icisip $icisport
    ;;

    "-w")
        orderip=$(sed -n '8p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') 
        orderport=$(sed -n '7p' config.json " " | sed 's/[^0-9]*//g')
        python3 compiled/WorkloadParser.py $orderip $orderport $2
    ;;

    "-p1")
        java -cp "./compiled/sqlite.jar:./compiled/json-20231013.jar" ./src/ProductService/ProductService.java $(sed -n '16p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') $(sed -n '12p' config.json " " | sed 's/[^0-9]*//g')
    ;;

    "-p2")
        java -cp "./compiled/sqlite.jar:./compiled/json-20231013.jar" ./src/ProductService/ProductService.java $(sed -n '16p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') $(sed -n '13p' config.json " " | sed 's/[^0-9]*//g')
    ;;

    "-p3")
        java -cp "./compiled/sqlite.jar:./compiled/json-20231013.jar" ./src/ProductService/ProductService.java $(sed -n '16p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') $(sed -n '14p' config.json " " | sed 's/[^0-9]*//g')
    ;;

    "-p4")    
        java -cp "./compiled/sqlite.jar:./compiled/json-20231013.jar" ./src/ProductService/ProductService.java $(sed -n '16p' config.json " " | sed -n 's/.*"ip": "\(.*\)"/\1/p') $(sed -n '15p' config.json " " | sed 's/[^0-9]*//g')
    ;;

    *)
        echo "Four"
    ;;
esac