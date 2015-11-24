import urllib.request
from bs4 import BeautifulSoup
import re, io, gzip, os, time

result_path = '/home/rui/Desktop/Skin/'
headers={'User-Agent': 'Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36',
         'Accept':' text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
         'Accept-Encoding': 'gzip'}

category = ['brands/dining/', 'athletes/nba/', 'organizations/']
pop_url = r'http://fanpagelist.com/category/' + category[1] + 'view/list/sort/fans/page'
place_url = r'https://www.facebook.com/places/'

outfile = "place.txt"

def fetch_html(url, retry):
    attempts = 0
    while attempts < retry:
        try:
            req = urllib.request.Request(url, headers=headers)
            response = urllib.request.urlopen(req)
            # accept encoding is gzip, decode to get the actual html
            bi = io.BytesIO(response.read())
            gf = gzip.GzipFile(fileobj=bi, mode='rb')
            return gf.read()

        except Exception as e:
            print(str(e))
            attempts += 1
            time.sleep(1)
    return ""

def fetch_pop_id(soup):
    page_lst = soup.findAll('img', {'class' : 'ranking_profile_image'})
    page_ids = {}
    for p in page_lst:
        page_ids[p['alt']] = p['src'].split('/')[3]
    save_as(outfile, page_ids)

def fetch_place_id(soup):
    place_lst = soup.findAll('a', {'class' : '_375k'})
    place_ids = {}
    for p in place_lst:
        place_ids[p.string] = p['href'].split('/')[-2]
    save_as(outfile, place_ids)

def save_as(filename, content):
    text_file = open(filename , "a")
    for k, v in content.items():
        text_file.write(v + '\t' + k + '\n')
    text_file.close()

def get_pop_pages():
    num_pages = 5
    for i in range(1, num_pages + 1):
        html = fetch_html(pop_url + str(i), 3)
        soup = BeautifulSoup(html, 'lxml')
        fetch_pop_id(soup)

def get_place_pages():
    html = fetch_html(place_url, 3)
    fetch_place_id(BeautifulSoup(html, 'lxml'))
