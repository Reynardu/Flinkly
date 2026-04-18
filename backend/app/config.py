from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str
    household_password: str
    secret_key: str

    class Config:
        env_file = ".env"


settings = Settings()
