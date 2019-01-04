@echo off

echo ==== Test Help Message ====
"C:\\python3\\python.exe" test-rabbitmq.py --help
echo.
echo ==== Test With Arguments ====
"C:\\python3\\python.exe" test-rabbitmq.py^
 --message="Hello From %~n0"^
 --host="localhost"^
 --port="5672"^
 --vhost="myvhost"^
 --username="guest"^
 --password="guest"
echo.