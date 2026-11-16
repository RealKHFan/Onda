Onda for Linux (debug build)

Install:  ./install.sh      (puts Onda in ~/.local/share/onda-app and your app menu; no root)
Or run without installing:  ./Onda/bin/Onda

Needs GStreamer, which Bazzite and other Fedora-based desktops already have.
Your settings and library list are kept in ~/.local/share/onda (readable only by you).

Phone link: if your phone can't find this PC, open Onda's port once:
  sudo firewall-cmd --permanent --add-port=45231/tcp --add-port=45231/udp && sudo firewall-cmd --reload

Copying new songs to a network share (SMB, for example a NAS): Onda can only use folders on this PC,
so mount the share once (replace 10.0.0.3, SHARE and YOURNAME with yours):
  sudo mkdir -p /var/mnt/nas /etc/samba
  sudo nano /etc/samba/nas-credentials          (two lines: username=YOURNAME and password=...)
  sudo chmod 600 /etc/samba/nas-credentials
  echo "//10.0.0.3/SHARE /var/mnt/nas cifs credentials=/etc/samba/nas-credentials,uid=$(id -u),gid=$(id -g),_netdev,nofail,x-systemd.automount,x-systemd.mount-timeout=10 0 0" | sudo tee -a /etc/fstab
  sudo systemctl daemon-reload && sudo systemctl restart remote-fs.target
Then choose a folder inside /var/mnt/nas in Onda. It reconnects by itself after a restart.
