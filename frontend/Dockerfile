# syntax=docker/dockerfile:1
# Front-end do MaisEdu: build com Vite e entrega com nginx, que também faz o proxy de /api
# para o back-end. Front e API ficam na mesma origem (cookie SameSite=Strict funciona, sem CORS).

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
