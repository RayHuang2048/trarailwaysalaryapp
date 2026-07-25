import requests
import json

client_id = "rayhuang2048-0aee86f6-a3c8-4d36"
client_secret = "7573bc0e-4e64-499e-9d0a-91899ef7b298"

# 1. Get Access Token
token_url = "https://tdx.transportdata.tw/auth/realms/TDXConnect/protocol/openid-connect/token"
payload = {
    "grant_type": "client_credentials",
    "client_id": client_id,
    "client_secret": client_secret
}
token_res = requests.post(token_url, data=payload)
token_data = token_res.json()
access_token = token_data.get("access_token")

if not access_token:
    print("Failed to get token")
    exit()

headers = {
    "Authorization": f"Bearer {access_token}",
    "Accept": "application/json"
}

# 2. Query ODFare V3 for Taipei (1000) to Kaohsiung (1210)
# This route has all train types.
url = "https://tdx.transportdata.tw/api/basic/v3/Rail/TRA/ODFare/1000/to/1210?$format=JSON"
res = requests.get(url, headers=headers)

if res.status_code == 200:
    data = res.json()
    od_fares = data.get("ODFares", [])
    print(f"Total ODFare groups: {len(od_fares)}")
    for i, od in enumerate(od_fares):
        tt = od.get("TrainType")
        dist = od.get("TravelDistance")
        print(f"Group {i}: TrainType={tt}, Distance={dist}km")
        fares = od.get("Fares", [])
        # Only print standard fares for brevity
        standard_fares = [f for f in fares if f.get("TicketType") == 1 and f.get("FareClass") == 1]
        for f in standard_fares:
            print(f"  - CabinClass={f.get('CabinClass')}, Price={f.get('Price')}")
else:
    print(f"Error {res.status_code}")
