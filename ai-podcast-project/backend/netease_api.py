import httpx
import logging

logger = logging.getLogger(__name__)

class NeteaseAPI:
    def __init__(self, base_url: str = "http://localhost:3000"):
        self.base_url = base_url
        self.client = httpx.AsyncClient(base_url=self.base_url)

    async def get_daily_recommend_songs(self, limit: int = 3):
        """
        获取每日推荐歌曲。如果未登录，默认抓取网易云热歌榜作为 fallback。
        """
        try:
            # 尝试调用每日推荐 (需要登录 cookies)
            response = await self.client.get("/recommend/songs")
            data = response.json()
            
            if data.get("code") == 200:
                songs = data.get("data", {}).get("dailySongs", [])[:limit]
                return self._parse_songs(songs)
            else:
                logger.warning("未登录或获取推荐失败，回退至热歌榜")
                return await self.get_hot_songs(limit)
        except Exception as e:
            logger.error(f"网易云接口调用异常: {e}")
            return await self.get_hot_songs(limit)

    async def get_hot_songs(self, limit: int = 3):
        """
        获取网易云热歌榜 (榜单 ID: 3778678)
        """
        try:
            response = await self.client.get("/playlist/track/all?id=3778678&limit=10")
            data = response.json()
            songs = data.get("songs", [])[:limit]
            return self._parse_songs(songs)
        except Exception as e:
            logger.warning(f"Fallback 失败，使用 Mock 数据: {e}")
            return [
                {"id": 1, "name": "Mock Song 1", "artist": "Mock Artist 1"},
                {"id": 2, "name": "Mock Song 2", "artist": "Mock Artist 2"}
            ][:limit]

    async def get_lyric(self, song_id: int):
        """
        获取歌曲的歌词
        """
        response = await self.client.get(f"/lyric?id={song_id}")
        data = response.json()
        return data.get("lrc", {}).get("lyric", "")

    def _parse_songs(self, songs_raw: list):
        """
        统一解析返回结构
        """
        parsed = []
        for song in songs_raw:
            parsed.append({
                "id": song["id"],
                "name": song["name"],
                "artist": ", ".join([ar["name"] for ar in song.get("ar", [])])
            })
        return parsed

    async def close(self):
        await self.client.aclose()
