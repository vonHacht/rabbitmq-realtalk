@echo off

echo ==== List of vhosts ====
curl -i -u guest:guest http://localhost:1337/api/vhosts
echo.
echo ==== List of channels ====
curl -i -u guest:guest "http://localhost:1337/api/channels?sort=message_stats.publish_details.rate&sort_reverse=true&columns=name,message_stats.publish_details.rate,message_stats.deliver_get_details.rate"
echo.
