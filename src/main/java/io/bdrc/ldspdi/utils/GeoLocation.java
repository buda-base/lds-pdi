package io.bdrc.ldspdi.utils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;

import io.bdrc.auth.AuthProps;
import io.bdrc.ldspdi.service.ServiceConfig;

public class GeoLocation {

    private static final String DBLocation = AuthProps.getProperty("geolite_countryDB");
    public static final String HEADER_NAME = "X-Real-IP";
    private static final Logger log = LoggerFactory.getLogger(GeoLocation.class);
    private static DatabaseReader dbReader;
    
    static {
        dbReader = getDbReader();
    }

    public static DatabaseReader getDbReader() {
        try {
            File database = new File(DBLocation);
            return new DatabaseReader.Builder(database).withCache(new CHMCache()).build();
        } catch (IOException e) {
            log.error("getDbReader()", e);
            return null;
        }
    }

    public static String getCountryCode(String ip) {
        try {
            final InetAddress ipAddress = InetAddress.getByName(ip);
            final CountryResponse response = dbReader.country(ipAddress);
            return response.getCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception e) {
            log.error("getCountryName()", e);
            return null;
        }
    }

    public static boolean isFromChina(final String addr) {
        if (ServiceConfig.isInChina()) return true;
        final String country = getCountryCode(addr);
        log.debug("For address {}, country is {}", addr, country);
        return (country == null || "CN".equalsIgnoreCase(country));
    }

    public static boolean isFromChina(HttpServletRequest request) {
        if (ServiceConfig.isInChina()) return true;
        final String addr = request.getHeader(HEADER_NAME);
        return isFromChina(addr);
    }
}