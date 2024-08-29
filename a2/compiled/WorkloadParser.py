import sys
import requests

IP = str(sys.argv[1])  # OrderService IP
PORT = str(sys.argv[2])  # OrderService Port Number
FILE = open(sys.argv[3], "r")  # Workload file to read from


ResetDatabase = True

line = FILE.readline().replace("\n", "").split(" ")

# Purge all data in database
if line[0] != "restart":
    headers = {
        "Content-Type": "application/json",
    }
    url = "http://" + IP + ":" + PORT + "/restart"
    response = requests.get(url, headers=headers)

while line != [""]:

    line.extend(["", "", "", "", "", ""])
    url = "http://" + IP + ":" + PORT + "/" + line[0].lower()
    headers = {
        "Content-Type": "application/json",
        "Authorization": "Bearer your_token",
    }
    if line[1].lower() == "create":  # Command is create
        if line[0].lower() == "user":  # Accessing user service
            response = requests.post(
                url,
                data={
                    "command": "create",
                    "id": line[2],
                    "username": line[3],
                    "email": line[4],
                    "password": line[5],
                },
                headers=headers,
            )
        else:  # Accessing product service
            response = requests.post(
                url,
                data={
                    "command": "create",
                    "id": line[2],
                    "name": line[3],
                    "description": line[4],
                    "price": line[5],
                    "quantity": line[6],
                },
                headers=headers,
            )

    elif line[1].lower() == "update":  # Command is update
        data = {"command": "update","id": line[2]}
        for update in line[2:]:
            data[update[0 : update.find(":")]] = update[update.find(":") + 1 :]
        []
        data.pop('')
        response = requests.post(
            url,
            data=data,
            headers=headers,
        )
    elif line[1].lower() == "delete":  # Command is delete
        if line[0].lower() == "user":  # Accessing user service
            response = requests.post(
                url,
                data={
                    "command": "delete",
                    "id": line[2],
                    "username": line[3],
                    "email": line[4],
                    "password": line[5],
                },
                headers=headers,
            )
        else:  # Accessing product service
            response = requests.post(
                url,
                data={
                    "command": "delete",
                    "id": line[2],
                    "name": line[3],
                    "description": line[4],
                    "price": line[5],
                    "quantity": line[6],
                },
                headers=headers,
            )
    elif line[1].lower() == "place":  # Command is place for order service
        response = requests.post(
                url,
                data={
                    "command": "place order",
                    "product_id": line[2],
                    "user_id": line[3],
                    "quantity": line[4],
                },
                headers=headers,
            )
        
    elif line[1].lower() == "get":  # Command is getting entity
        response = requests.get(url + "/" + line[2], headers=headers)

    elif line[1].lower() == "info":  # Command is getting entity
        response = requests.get(url + "/" + line[2], headers=headers)

    elif line[0] == "shutdown":
        ResetDatabase = False
        url = "http://" + IP + ":" + PORT + "/poweroff"
        response = requests.get(url, headers=headers)
    
    else:
        line = FILE.readline().replace("\n", "").split(" ")
        continue
    line = FILE.readline().replace("\n", "").split(" ")

if (ResetDatabase):
    headers = {"Content-Type": "application/json"}
    url = "http://" + IP + ":" + PORT + "/restart"
    response = requests.get(url, headers=headers)