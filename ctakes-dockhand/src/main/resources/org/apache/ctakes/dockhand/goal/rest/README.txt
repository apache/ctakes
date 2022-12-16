To create a tiny rest server:

1.  Execute "docker-machine env".
     This will list the full DOCKER_HOST address.
     e.g. "tcp://192.168.99.101:2376"

2.  Make note of the address between "//" and ":".
     e.g. "192.168.99.101".
     Let us call this "IP.IP.IP.IP".

3.  Change to the directory containing "Dockerfile".

4.  Execute "docker build -t ctakes_tiny_rest .".
     This will build an image with the tag "ctakes_tiny_rest".

5.  Set the environment variable "umlsUser" to your umls username.

6.  Set the environment variable "umlsPass" to your umls password.

5.  Execute "docker run --name my_ctakes_rest --rm -d -p 8080:8080 -e umlsUser -e umlsPass ctakes_tiny_rest".
     This will start a container named "my_ctakes_rest" that runs the server.

6.  In a browser, visit "http://IP.IP.IP.IP:8080/ctakes_tiny_rest".
     This should open a ctakes demo front page.


7.  To stop the server, execute "docker stop my_ctakes_rest".

8.  To remove the image, execute "docker rmi ctakes_tiny_rest".

9.  To clean your docker machine, execute "docker system prune".
