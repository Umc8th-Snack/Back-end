name: Deploy to EC2

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v3

      - name: Set up SSH key
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.SSH_PRIVATE_KEY }}" > ~/.ssh/github_key
          chmod 600 ~/.ssh/github_key

      - name: Create .env file
        run: |
          echo "SPRING_PROFILES_ACTIVE=${{ secrets.SPRING_PROFILES_ACTIVE }}" >> .env
          echo "RDS_URL=${{ secrets.RDS_URL }}" >> .env
          echo "RDS_USERNAME=${{ secrets.RDS_USERNAME }}" >> .env
          echo "RDS_PASSWORD=${{ secrets.RDS_PASSWORD }}" >> .env
          echo "JWT_SECRET_KEY=${{ secrets.JWT_SECRET_KEY }}" >> .env
          echo "GOOGLE_API_KEY=${{ secrets.GOOGLE_API_KEY }}" >> .env

      - name: Upload .env and deploy via SSH
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USERNAME }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          port: 22
          script: |
            cd /home/ubuntu/snack
            git pull origin main
            echo "${{ secrets.SPRING_PROFILES_ACTIVE }}" > .env
            echo "${{ secrets.RDS_URL }}" >> .env
            echo "${{ secrets.RDS_USERNAME }}" >> .env
            echo "${{ secrets.RDS_PASSWORD }}" >> .env
            echo "${{ secrets.JWT_SECRET_KEY }}" >> .env
            echo "${{ secrets.GOOGLE_API_KEY }}" >> .env
            ./gradlew build
            sudo systemctl restart snack.service