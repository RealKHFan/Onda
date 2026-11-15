# Your own music server (Navidrome)

Onda streams your whole library from a Navidrome server on your Raspberry Pi or NAS, at home and, over WireGuard, when you're out.

## Raspberry Pi (Docker)
1. Install Docker: `curl -fsSL https://get.docker.com | sh`
2. Create `~/navidrome/docker-compose.yml`:
   ```yaml
   services:
     navidrome:
       image: deluan/navidrome:latest
       user: 1000:1000
       ports:
         - "4533:4533"
       restart: unless-stopped
       volumes:
         - ./data:/data
         - /path/to/your/music:/music:ro
   ```
3. `cd ~/navidrome && docker compose up -d`, open `http://<pi-address>:4533` and create your user.

## Synology NAS
Container Manager → Registry → `deluan/navidrome` → map port 4533, a `data` folder to `/data` and your music share to `/music` (read-only).

## In Onda
Settings → Music server: `http://<pi-or-nas-address>:4533`, your Navidrome user and password. Onda stores only a salted token, never the password. Your server's playlists show up in Your Library.

## Away from home
Connect the phone to your home network with WireGuard (for example PiVPN on the Pi), then use the same LAN address. Without a VPN, put the server behind HTTPS before opening it to the internet.
