import http from 'k6/http';

export const options = {
    vus: 3,          // 3 virtual users
    duration: '10s', // X time
    iterations: 85, // Total requests across all VUs
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    const params = {
        headers: {
            'x-api-key': 'key-1',
        },
    };


    http.get(`${BASE_URL}/api/v1/data`, params);
}