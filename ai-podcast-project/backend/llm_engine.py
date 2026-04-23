import os
import json
import logging
from openai import AsyncOpenAI

logger = logging.getLogger(__name__)

class LLMEngine:
    def __init__(self):
        # 默认使用环境变量配置 OpenAI (或者兼容 OpenAI 格式的其他大模型, 如 DeepSeek/Kimi)
        self.api_key = os.getenv("OPENAI_API_KEY", "your-api-key")
        self.base_url = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")
        self.model = os.getenv("OPENAI_MODEL", "gpt-4-turbo-preview")
        self.client = AsyncOpenAI(api_key=self.api_key, base_url=self.base_url)

    async def generate_dj_script(self, songs_info: list, context: str = "早间通勤", target_language_ratio: str = "70% 中文，30% 英文") -> dict:
        """
        调用大语言模型，生成 AI DJ 播客的串场台本和英语教学内容
        """
        
        songs_str = json.dumps(songs_info, ensure_ascii=False, indent=2)
        
        prompt = f"""
        你是一个名为 "Echo" 的私人 AI 双语音乐电台 DJ。你的性格幽默风趣、富有同理心，并且热爱教人英语。
        
        当前用户所处的场景/状态是：{context}
        即将播放的歌曲列表（包含歌名、歌手和部分歌词）：
        {songs_str}

        你的任务是生成一期简短的电台节目台本。要求如下：
        1. 包含一段符合当前场景的开场白（Greeting）。
        2. 为列表中的每首歌生成一段“串场词”（Transition）。在串场词中，你需要自然地从下一首歌的歌词中提取 1~2 个实用的英文单词或短语，教给听众（不要生硬地背单词，而是结合歌曲意境解释用法）。
        3. 如果歌曲是纯中文且没有英文歌词，请教一个与该歌曲情感或意境相关的英文单词。
        4. 语言比例控制在：{target_language_ratio}。
        5. 语气必须像真实的播客主持人在讲话，有呼吸感，像在陪伴朋友。
        6. 包含一段结束语（Outro）。
        
        请严格按照以下 JSON 格式输出，不要输出任何额外的 Markdown 标记：
        {{
            "episodes": [
                {{
                    "type": "dj_talk",
                    "speaker": "Echo",
                    "content": "开场白文本...",
                    "learning_words": []
                }},
                {{
                    "type": "song",
                    "song_id": 12345,
                    "song_name": "歌曲名",
                    "artist": "歌手名"
                }},
                {{
                    "type": "dj_talk",
                    "speaker": "Echo",
                    "content": "关于下一首歌的串场词与英语教学...",
                    "learning_words": [
                        {{"word": "heartbreak", "meaning": "心碎", "example": "This song is about a terrible heartbreak."}}
                    ]
                }},
                ...
            ]
        }}
        """

        logger.info("正在调用 LLM 生成 AI DJ 台本...")
        try:
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "你是一个专业的 JSON 数据生成引擎，只输出合法的 JSON 字符串。"},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.7,
                response_format={ "type": "json_object" } # 强制输出 JSON
            )
            
            result_text = response.choices[0].message.content
            return json.loads(result_text)
            
        except Exception as e:
            logger.error(f"LLM 调用失败: {e}")
            # 返回一个基础的 Fallback 台本以防崩溃
            return self._generate_fallback_script(songs_info)

    def _generate_fallback_script(self, songs_info: list) -> dict:
        episodes = [
            {"type": "dj_talk", "speaker": "Echo", "content": "Hello! It seems my AI brain is taking a quick nap. But don't worry, let's just enjoy the music!", "learning_words": []}
        ]
        for song in songs_info:
            episodes.append({
                "type": "song",
                "song_id": song["id"],
                "song_name": song["name"],
                "artist": song["artist"]
            })
        return {"episodes": episodes}
