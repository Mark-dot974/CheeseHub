package com.mark.community;

import org.junit.Test;

import java.util.Scanner;

import java.util.Scanner;

/**
 * @author 作者 gudaochangsheng
 * @version 创建时间：2019年10月9日 上午9:25:38
 *
 */
public class Nextdate {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        int year;
        int month;
        int day;
        Scanner in = new Scanner(System.in);
        System.out.println("year:");
        year = in.nextInt();
        while(!(year>=1000&&year<=9999))
        {
            System.out.println("error");
            return;
        }
        System.out.println("month");
        month = in.nextInt();
        while(month>12||month<1)
        {
            System.out.println("error");
            return;
        }
        System.out.println("day:");
        day = in.nextInt();
        if(month==1||month==3||month==5||month==7||month==8||month==10||month==12)
        {
            while(day>31||day<1)
            {
                System.out.println("error");
                return;
            }
        }
        if(month==4||month==6||month==9||month==11)
        {
            while(day>30||day<1)
            {
                System.out.println("error");
                return;
            }
        }
        if((year%4==0&&year%100!=0)||(year%400==0))
        {
            if(month==2)
            {
                if(day>29)
                {
                    System.out.println("error");
                    return;
                }
                else
                {
                    if(day==29)
                    {
                        day=1;
                    }
                    else
                    {
                        day++;
                    }
                }
            }
        }
        else
        {
            if(month==2)
            {
                if(day>28)
                {
                    System.out.println("error");
                    return;
                }
                else
                {
                    if(day==28)
                    {
                        day=1;
                    }
                    else
                    {
                        day++;
                    }
                }
            }
        }
        switch (month) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
                if(day==31)
                {
                    month++;
                    day=1;
                    System.out.println(year+" "+month+" "+day);
                    break;
                }
                else
                {
                    day++;
                    System.out.println(year+" "+month+" "+day);
                    break;
                }
            case 2:
                if(day==1)
                {
                    month++;
                    System.out.println(year+" "+month+" "+day);
                    break;
                }
                else
                {
                    System.out.println(year+" "+month+" "+day);
                    break;
                }
            case 4:
            case 6:
            case 9:
            case 11:
                if(day==30)
                {
                    month++;
                    day=1;
                    System.out.println(year+" "+month+" "+day);
                    break;
                }
                else
                {
                    day++;
                    System.out.println(year+" "+month+" "+day);
                    break;
                }
            case 12:
                if(day==31)
                {
                    year++;
                    month=1;
                    day=1;
                    System.out.println(year+" "+month+" "+day);
                    break;
                }
                else
                {
                    day++;
                    System.out.println(year+" "+month+" "+day);
                    break;
                }
        }
    }
}

