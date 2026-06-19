PKG=com.tencent.tmgp.pubgmhd
ID=$(grep $PKG /data/system/users/0/settings_ssaid.xml | awk -F'"' '{print $6}')
for i in $(seq 16)
do P=$P$(uuidgen|head -c 1|tr '-' -d)
done
sed -i s/$ID/$P/g /data/system/users/0/settings_ssaid.xml

echo -e "\033[41m---安卓id更改成功---\033[0m"
echo -e "\033[41m---重启后生效---\033[0m"
echo -e "\033[41m---务必安装游戏并打开一次游戏后，安卓id修改才生效!!!---\033[0m"
echo -e "\033[41m---修改后再次执行一次清理游戏sh并重启设备---\033[0m"
echo -e "\033[35m 如果有异样提示，则修改不成功，建议百度或者酷安app下载爱玩机工具箱或者设备id修改器，自行修改，修改后并重启   \033[5m"
echo -e "\033[41m---公益频道 @aadaili---\033[0m"